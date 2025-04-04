package com.example.samsunggearcontrollerbleandroiddriver.model

import android.bluetooth.BluetoothDevice

/**
 * Data class representing a Samsung Gear Controller device.
 */
data class ControllerDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String
)
