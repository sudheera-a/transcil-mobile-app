/**
 * UI state for the Bluetooth screen; forwards connect/scan/send to repository + service.
 */
package com.transcil.rider.bluetooth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class BluetoothViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BluetoothRepository(app.applicationContext)

    val paired: LiveData<List<BluetoothDeviceItem>> = repo.paired
    val available: LiveData<List<BluetoothDeviceItem>> = repo.available
    val state = BluetoothLink.state
    val deviceName = BluetoothLink.deviceName
    val console = BluetoothLink.console
    val toast = BluetoothLink.toast

    fun onStart() = repo.register()

    fun onStop() = repo.unregister()

    fun isSupported() = repo.isSupported()

    fun hasConnectPermission() = repo.hasConnectPermission()

    fun hasScanPermission() = repo.hasScanPermission()

    fun refreshPaired() = repo.refreshPaired()

    /** @return false if discovery could not start */
    fun startScan(): Boolean = repo.startScan()

    fun stopScan() = repo.stopScan()

    fun isAdapterEnabled() = repo.isAdapterEnabled()

    fun isLocationEnabled() = repo.isLocationEnabled()

    fun enableIntent() = repo.enableIntent()

    fun bluetoothSettingsIntent() = repo.bluetoothSettingsIntent()

    fun locationSettingsIntent() = repo.locationSettingsIntent()

    fun disableAdapter() = repo.disableAdapter()

    fun refreshAdapterState() = repo.refreshAdapterState()

    fun connect(item: BluetoothDeviceItem) {
        // Discovery/LE scan interferes with connect on many OEMs — stop first.
        repo.stopScan()
        BluetoothConnectionService.connect(getApplication(), item.address, item.name)
    }

    fun disconnect() = BluetoothConnectionService.disconnect(getApplication())

    fun send(text: String) = BluetoothConnectionService.send(getApplication(), text)

    fun tryRestore() = BluetoothConnectionService.tryRestore(getApplication())

    fun clearToast() = BluetoothLink.clearToast()
}
