package com.example.samsunggearcontrollerbleandroiddriver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.samsunggearcontrollerbleandroiddriver.MainActivity
import com.example.samsunggearcontrollerbleandroiddriver.R
import com.example.samsunggearcontrollerbleandroiddriver.bluetooth.BluetoothManager
import com.example.samsunggearcontrollerbleandroiddriver.model.ControllerDevice
import com.example.samsunggearcontrollerbleandroiddriver.model.ControllerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service for maintaining the BLE connection with the Samsung Gear Controller.
 */
class GearControllerService : Service() {

    companion object {
        private const val TAG = "GearControllerService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "controller_service_channel"

        // Intent actions
        const val ACTION_DISCONNECT = "com.example.samsunggearcontrollerbleandroiddriver.ACTION_DISCONNECT"
    }

    // Binder for client communication
    private val binder = LocalBinder()

    // Coroutine scope for service operations
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Bluetooth manager
    private lateinit var bluetoothManager: BluetoothManager

    // Pointer overlay manager
    private lateinit var pointerOverlayManager: PointerOverlayManager

    // Controller state
    private val _controllerState = MutableStateFlow(ControllerState())
    val controllerState: StateFlow<ControllerState> = _controllerState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        // Initialize Bluetooth manager
        bluetoothManager = BluetoothManager(this)

        // Initialize pointer overlay manager
        pointerOverlayManager = PointerOverlayManager(this)

        // Collect controller state updates
        serviceScope.launch(Dispatchers.Main) {
            bluetoothManager.controllerState.collectLatest { state ->
                _controllerState.value = state

                // Update pointer position based on gyroscope data
                if (state.isMouseMoving) {
                    pointerOverlayManager.updatePointerPosition(state.gyroX, state.gyroY)
                }

                // Forward controller state to accessibility service
                val intent = Intent(this@GearControllerService, GearControllerAccessibilityService::class.java)
                intent.action = GearControllerAccessibilityService.ACTION_UPDATE_CONTROLLER_STATE
                intent.putExtra(GearControllerAccessibilityService.EXTRA_VOLUME_UP, state.volumeUp)
                intent.putExtra(GearControllerAccessibilityService.EXTRA_VOLUME_DOWN, state.volumeDown)
                intent.putExtra(GearControllerAccessibilityService.EXTRA_HOME, state.home)
                intent.putExtra(GearControllerAccessibilityService.EXTRA_BACK, state.back)
                intent.putExtra(GearControllerAccessibilityService.EXTRA_TRIGGER, state.trigger)
                intent.putExtra(GearControllerAccessibilityService.EXTRA_TOUCHPAD_CLICK, state.touchpadClick)
                intent.putExtra(GearControllerAccessibilityService.EXTRA_TOUCHPAD_X, state.touchpadX)
                intent.putExtra(GearControllerAccessibilityService.EXTRA_TOUCHPAD_Y, state.touchpadY)
                sendBroadcast(intent)
            }
        }

        // Collect connection state updates
        serviceScope.launch(Dispatchers.Main) {
            bluetoothManager.connectionState.collectLatest { state ->
                when (state) {
                    BluetoothManager.ConnectionState.CONNECTED -> {
                        // Show the pointer overlay when connected
                        pointerOverlayManager.showPointer()

                        // Update notification
                        updateNotification()
                    }
                    BluetoothManager.ConnectionState.DISCONNECTED -> {
                        // Hide the pointer overlay when disconnected
                        pointerOverlayManager.hidePointer()

                        // Update notification
                        updateNotification()
                    }
                    else -> {
                        // Update notification
                        updateNotification()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        // Handle intent actions
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                // Disconnect from the controller
                bluetoothManager.disconnect()

                // Stop the service
                stopSelf()
            }
        }

        // Create notification channel
        createNotificationChannel()

        // Start as a foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        // Disconnect from the controller
        bluetoothManager.disconnect()

        // Hide the pointer overlay
        pointerOverlayManager.hidePointer()

        // Cancel coroutines
        serviceScope.cancel()
    }

    /**
     * Connects to the specified controller device.
     */
    fun connect(device: ControllerDevice) {
        bluetoothManager.connect(device)
    }

    /**
     * Disconnects from the current controller device.
     */
    fun disconnect() {
        bluetoothManager.disconnect()
    }

    /**
     * Creates the notification channel for the service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val description = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the notification for the foreground service.
     */
    private fun createNotification(): Notification {
        // Create intent for the notification
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Create disconnect intent
        val disconnectIntent = Intent(this, GearControllerService::class.java).apply {
            action = ACTION_DISCONNECT
        }

        val disconnectPendingIntent = PendingIntent.getService(
            this,
            0,
            disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getNotificationText())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.disconnect), disconnectPendingIntent)
            .build()
    }

    /**
     * Updates the notification with the current connection state.
     */
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    /**
     * Gets the notification text based on the current connection state.
     */
    private fun getNotificationText(): String {
        val device = bluetoothManager.connectedDevice.value

        return when (bluetoothManager.connectionState.value) {
            BluetoothManager.ConnectionState.CONNECTED -> {
                getString(R.string.notification_text, device?.address ?: "")
            }
            BluetoothManager.ConnectionState.CONNECTING -> {
                getString(R.string.connecting)
            }
            BluetoothManager.ConnectionState.DISCONNECTED -> {
                getString(R.string.disconnected)
            }
        }
    }

    /**
     * Binder class for client communication.
     */
    inner class LocalBinder : Binder() {
        fun getService(): GearControllerService = this@GearControllerService
    }
}
