package com.example.samsunggearcontrollerbleandroiddriver

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samsunggearcontrollerbleandroiddriver.bluetooth.BluetoothManager as AppBluetoothManager
import com.example.samsunggearcontrollerbleandroiddriver.databinding.ActivityMainBinding
import com.example.samsunggearcontrollerbleandroiddriver.model.ControllerDevice
import com.example.samsunggearcontrollerbleandroiddriver.model.ControllerState
import com.example.samsunggearcontrollerbleandroiddriver.model.TouchpadDirection
import com.example.samsunggearcontrollerbleandroiddriver.service.GearControllerService
import com.example.samsunggearcontrollerbleandroiddriver.ui.DeviceAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_PERMISSIONS = 2

        // Test device MAC address
        private const val TEST_DEVICE_MAC = "AB:1A:12:BA:BA:2C"
    }

    // View binding
    private lateinit var binding: ActivityMainBinding

    // Bluetooth manager
    private lateinit var bluetoothManager: AppBluetoothManager

    // Device adapter
    private lateinit var deviceAdapter: DeviceAdapter

    // Service connection
    private var controllerService: GearControllerService? = null
    private var bound = false

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            loadPairedDevices()
        } else {
            showPermissionRationale()
        }
    }

    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as GearControllerService.LocalBinder
            controllerService = binder.getService()
            bound = true

            // Observe controller state
            observeControllerState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            controllerService = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Bluetooth manager
        bluetoothManager = AppBluetoothManager(this)

        // Initialize device adapter
        deviceAdapter = DeviceAdapter { device ->
            connectToDevice(device)
        }

        // Set up RecyclerView
        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }

        // Set up disconnect button
        binding.btnDisconnect.setOnClickListener {
            disconnectFromDevice()
        }

        // Check permissions
        checkPermissions()
    }

    override fun onStart() {
        super.onStart()

        // Bind to the service
        Intent(this, GearControllerService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()

        // Unbind from the service
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if Bluetooth is enabled
        checkBluetoothEnabled()

        // Check if Accessibility Service is enabled
        checkAccessibilityServiceEnabled()

        // Check if overlay permission is granted
        checkOverlayPermission()
    }

    /**
     * Checks if the required permissions are granted.
     */
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        // Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // Location permission (required for BLE scanning)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        // Check if permissions are granted
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            // Request permissions
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            // Permissions already granted, load paired devices
            loadPairedDevices()
        }
    }

    /**
     * Shows a dialog explaining why permissions are needed.
     */
    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Bluetooth and location permissions are required to connect to the Samsung Gear Controller.")
            .setPositiveButton("Grant") { _, _ ->
                checkPermissions()
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this, "Permissions are required to use this app", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    /**
     * Checks if Bluetooth is enabled.
     */
    private fun checkBluetoothEnabled() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "This device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth is not enabled, request to enable it
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }

    /**
     * Checks if the Accessibility Service is enabled.
     */
    private fun checkAccessibilityServiceEnabled() {
        val accessibilityEnabled = isAccessibilityServiceEnabled()

        if (!accessibilityEnabled) {
            // Show dialog to enable Accessibility Service
            AlertDialog.Builder(this)
                .setTitle("Accessibility Service Required")
                .setMessage(getString(R.string.permission_accessibility_rationale))
                .setPositiveButton(getString(R.string.enable_accessibility)) { _, _ ->
                    // Open Accessibility settings
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    Toast.makeText(this, "Accessibility Service is required for input simulation", Toast.LENGTH_LONG).show()
                }
                .show()
        }
    }

    /**
     * Checks if the overlay permission is granted.
     */
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Show dialog to enable overlay permission
            AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage(getString(R.string.permission_overlay_rationale))
                .setPositiveButton(getString(R.string.enable_overlay)) { _, _ ->
                    // Open overlay settings
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    Toast.makeText(this, "Overlay permission is required for mouse pointer", Toast.LENGTH_LONG).show()
                }
                .show()
        }
    }

    /**
     * Checks if the Accessibility Service is enabled.
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )

        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )

            if (services != null) {
                return services.contains(packageName + "/" + packageName + ".service.GearControllerAccessibilityService")
            }
        }

        return false
    }

    /**
     * Loads the list of paired Samsung Gear Controllers.
     */
    private fun loadPairedDevices() {
        val pairedDevices = bluetoothManager.getPairedControllers()

        // Update the adapter
        deviceAdapter.submitList(pairedDevices)

        // Show/hide no devices message
        binding.tvNoDevices.visibility = if (pairedDevices.isEmpty()) View.VISIBLE else View.GONE

        // Check if the test device is in the list
        val testDevice = pairedDevices.find { it.address == TEST_DEVICE_MAC }
        if (testDevice != null) {
            Log.d(TAG, "Found test device: ${testDevice.name} (${testDevice.address})")
            // Optionally auto-connect to the test device
            // connectToDevice(testDevice)
        }
    }

    /**
     * Connects to the specified controller device.
     */
    private fun connectToDevice(device: ControllerDevice) {
        Log.d(TAG, "Connecting to device: ${device.name} (${device.address})")

        // Start the service
        val serviceIntent = Intent(this, GearControllerService::class.java)
        startService(serviceIntent)

        // Connect to the device
        controllerService?.connect(device)

        // Update UI
        binding.tvMacAddress.text = getString(R.string.device_mac, device.address)
        binding.btnDisconnect.isEnabled = true
    }

    /**
     * Disconnects from the current controller device.
     */
    private fun disconnectFromDevice() {
        Log.d(TAG, "Disconnecting from device")

        // Disconnect from the device
        controllerService?.disconnect()

        // Stop the service
        val serviceIntent = Intent(this, GearControllerService::class.java)
        stopService(serviceIntent)

        // Update UI
        binding.tvMacAddress.text = getString(R.string.device_mac, "")
        binding.btnDisconnect.isEnabled = false
        resetControllerStateUI()
    }

    /**
     * Observes the controller state and updates the UI.
     */
    private fun observeControllerState() {
        lifecycleScope.launch {
            controllerService?.controllerState?.collectLatest { state ->
                updateControllerStateUI(state)
            }
        }
    }

    /**
     * Updates the UI with the current controller state.
     */
    private fun updateControllerStateUI(state: ControllerState) {
        // Update button states
        binding.tvVolumeUp.text = getString(R.string.button_volume_up, state.volumeUp.toString())
        binding.tvVolumeDown.text = getString(R.string.button_volume_down, state.volumeDown.toString())
        binding.tvHome.text = getString(R.string.button_home, state.home.toString())
        binding.tvBack.text = getString(R.string.button_back, state.back.toString())
        binding.tvTrigger.text = getString(R.string.button_trigger, state.trigger.toString())
        binding.tvTouchpadClick.text = getString(R.string.touchpad_click, state.touchpadClick.toString())
        binding.tvMouseMovement.text = getString(R.string.mouse_movement, state.isMouseMoving.toString())

        // Update touchpad cross
        updateTouchpadCross(state.getTouchpadDirection())
    }

    /**
     * Updates the touchpad cross UI based on the active direction.
     */
    private fun updateTouchpadCross(direction: TouchpadDirection?) {
        // Reset all arrows to gray
        binding.ivTouchpadCross.drawable.setTint(ContextCompat.getColor(this, android.R.color.darker_gray))

        // Highlight the active direction
        if (direction != null) {
            // In a real implementation, we would highlight the specific arrow
            // This would require a custom drawable with separate paths for each arrow
        }
    }

    /**
     * Resets the controller state UI to default values.
     */
    private fun resetControllerStateUI() {
        updateControllerStateUI(ControllerState())
    }
}