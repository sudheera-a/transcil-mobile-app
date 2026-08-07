package com.transcil.rider.bluetooth

data class BluetoothDeviceItem(
    val address: String,
    val name: String,
    val bonded: Boolean,
)
