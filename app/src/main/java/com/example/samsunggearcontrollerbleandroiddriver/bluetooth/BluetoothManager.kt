package com.example.samsunggearcontrollerbleandroiddriver.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.samsunggearcontrollerbleandroiddriver.model.ControllerDevice
import com.example.samsunggearcontrollerbleandroiddriver.model.ControllerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manager class for handling Bluetooth LE connections with the Samsung Gear Controller.
 */
class BluetoothManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothManager"
        
        // Samsung Gear Controller service and characteristic UUIDs
        // Based on reverse engineering data from https://jsyang.ca/hacks/gear-vr-rev-eng/
        private val CONTROLLER_SERVICE_UUID = UUID.fromString("4f63756c-7573-2054-6872-65656d6f7465")
        private val CONTROLLER_CHARACTERISTIC_UUID = UUID.fromString("c8c51726-81bc-483b-a052-f7a14ea3d281")
        private val CONTROLLER_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        
        // Device name filter for Samsung Gear Controller
        private const val DEVICE_NAME_FILTER = "Gear VR Controller"
    }
    
    // Bluetooth adapter
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    
    // GATT connection
    private var bluetoothGatt: BluetoothGatt? = null
    
    // Handler for delayed reconnection attempts
    private val handler = Handler(Looper.getMainLooper())
    
    // Connection state
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Controller state
    private val _controllerState = MutableStateFlow(ControllerState())
    val controllerState: StateFlow<ControllerState> = _controllerState.asStateFlow()
    
    // Connected device
    private val _connectedDevice = MutableStateFlow<ControllerDevice?>(null)
    val connectedDevice: StateFlow<ControllerDevice?> = _connectedDevice.asStateFlow()
    
    /**
     * Gets a list of paired Samsung Gear Controllers.
     */
    fun getPairedControllers(): List<ControllerDevice> {
        if (bluetoothAdapter == null) return emptyList()
        
        return bluetoothAdapter.bondedDevices
            .filter { it.name?.contains(DEVICE_NAME_FILTER) == true }
            .map { device ->
                ControllerDevice(
                    device = device,
                    name = device.name ?: "Unknown Device",
                    address = device.address
                )
            }
    }
    
    /**
     * Connects to the specified controller device.
     */
    fun connect(device: ControllerDevice) {
        if (bluetoothAdapter == null || device.address.isEmpty()) {
            Log.e(TAG, "BluetoothAdapter not initialized or invalid address")
            return
        }
        
        // Update connection state
        _connectionState.value = ConnectionState.CONNECTING
        
        // Connect to the device
        bluetoothGatt = device.device.connectGatt(context, false, gattCallback)
        
        // Update connected device
        _connectedDevice.value = device
        
        Log.d(TAG, "Connecting to ${device.name} (${device.address})")
    }
    
    /**
     * Disconnects from the current controller device.
     */
    fun disconnect() {
        bluetoothGatt?.let { gatt ->
            gatt.disconnect()
            gatt.close()
            bluetoothGatt = null
        }
        
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDevice.value = null
        
        Log.d(TAG, "Disconnected from controller")
    }
    
    /**
     * Attempts to reconnect to the last connected device.
     */
    fun reconnect() {
        val device = _connectedDevice.value ?: return
        
        Log.d(TAG, "Attempting to reconnect to ${device.name} (${device.address})")
        
        // Disconnect first if already connected
        bluetoothGatt?.close()
        bluetoothGatt = null
        
        // Reconnect
        _connectionState.value = ConnectionState.CONNECTING
        bluetoothGatt = device.device.connectGatt(context, false, gattCallback)
    }
    
    /**
     * GATT callback for handling BLE events.
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    _connectionState.value = ConnectionState.CONNECTED
                    
                    // Discover services
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    
                    // Schedule reconnection attempt
                    handler.postDelayed({ reconnect() }, 5000)
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                
                // Find the controller service and characteristic
                val service = gatt.getService(CONTROLLER_SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(CONTROLLER_CHARACTERISTIC_UUID)
                    if (characteristic != null) {
                        // Enable notifications
                        gatt.setCharacteristicNotification(characteristic, true)
                        
                        // Write descriptor to enable notifications
                        val descriptor = characteristic.getDescriptor(CONTROLLER_DESCRIPTOR_UUID)
                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(it)
                        }
                    } else {
                        Log.e(TAG, "Controller characteristic not found")
                    }
                } else {
                    Log.e(TAG, "Controller service not found")
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == CONTROLLER_CHARACTERISTIC_UUID) {
                // Parse the controller data
                parseControllerData(characteristic.value)
            }
        }
    }
    
    /**
     * Parses the controller data from the BLE characteristic.
     * 
     * This implementation is based on the reverse engineering data from:
     * https://jsyang.ca/hacks/gear-vr-rev-eng/
     */
    private fun parseControllerData(data: ByteArray) {
        if (data.size < 60) {
            Log.e(TAG, "Invalid data size: ${data.size}")
            return
        }
        
        try {
            // Parse button states
            val buttonState = data[58].toInt() and 0xFF
            
            val volumeUp = (buttonState and 0x01) != 0
            val volumeDown = (buttonState and 0x02) != 0
            val home = (buttonState and 0x04) != 0
            val back = (buttonState and 0x08) != 0
            val touchpadClick = (buttonState and 0x10) != 0
            val trigger = (buttonState and 0x20) != 0
            
            // Parse touchpad coordinates
            val touchpadX = data[53].toInt().toFloat() / 127.0f
            val touchpadY = data[54].toInt().toFloat() / 127.0f
            
            // Parse gyroscope data
            val gyroX = bytesToShort(data, 4).toFloat() / 10000.0f
            val gyroY = bytesToShort(data, 6).toFloat() / 10000.0f
            val gyroZ = bytesToShort(data, 8).toFloat() / 10000.0f
            
            // Parse accelerometer data
            val accelX = bytesToShort(data, 10).toFloat() / 10000.0f
            val accelY = bytesToShort(data, 12).toFloat() / 10000.0f
            val accelZ = bytesToShort(data, 14).toFloat() / 10000.0f
            
            // Determine if mouse is moving based on gyroscope data
            val isMouseMoving = Math.abs(gyroX) > 0.01f || Math.abs(gyroY) > 0.01f
            
            // Update controller state
            _controllerState.value = ControllerState(
                volumeUp = volumeUp,
                volumeDown = volumeDown,
                home = home,
                back = back,
                trigger = trigger,
                touchpadClick = touchpadClick,
                touchpadX = touchpadX,
                touchpadY = touchpadY,
                gyroX = gyroX,
                gyroY = gyroY,
                gyroZ = gyroZ,
                accelX = accelX,
                accelY = accelY,
                accelZ = accelZ,
                isMouseMoving = isMouseMoving
            )
            
            Log.d(TAG, "Controller state updated: ${_controllerState.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing controller data", e)
        }
    }
    
    /**
     * Converts two bytes to a short value.
     */
    private fun bytesToShort(bytes: ByteArray, offset: Int): Short {
        return ((bytes[offset + 1].toInt() and 0xFF) shl 8 or (bytes[offset].toInt() and 0xFF)).toShort()
    }
    
    /**
     * Enum representing the connection states.
     */
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}
