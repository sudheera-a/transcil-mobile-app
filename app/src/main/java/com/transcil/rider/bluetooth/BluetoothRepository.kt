/**
 * Classic Bluetooth adapter helpers: enable state, bonded devices, discovery scan.
 */
package com.transcil.rider.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class BluetoothRepository(private val appContext: Context) {

    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val _paired = MutableLiveData<List<BluetoothDeviceItem>>(emptyList())
    val paired: LiveData<List<BluetoothDeviceItem>> = _paired

    private val _available = MutableLiveData<List<BluetoothDeviceItem>>(emptyList())
    val available: LiveData<List<BluetoothDeviceItem>> = _available

    private val found = LinkedHashMap<String, BluetoothDeviceItem>()
    private var receiverRegistered = false
    private var leScanning = false

    // Classic discovery only sees classic devices in discoverable mode; most real
    // hardware (BMS modules, wearables, phones) advertises over BLE instead.
    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address ?: return
            if (_paired.value.orEmpty().any { it.address == address }) return
            val name = result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
                ?: runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: found[address]?.name
                ?: address
            found[address] = BluetoothDeviceItem(address, name, bonded = false)
            publishAvailable()
        }
    }

    private fun publishAvailable() {
        _available.postValue(dedupeForDisplay(found.values, _paired.value.orEmpty()))
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                // Name often arrives after ACTION_FOUND, via ACTION_NAME_CHANGED.
                BluetoothDevice.ACTION_FOUND,
                BluetoothDevice.ACTION_NAME_CHANGED,
                -> upsertFound(intent)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    stopLeScan()
                    if (BluetoothLink.state.value == BluetoothConnectionState.Scanning) {
                        BluetoothLink.setState(BluetoothConnectionState.On)
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    refreshAdapterState()
                    if (adapter?.isEnabled == true) refreshPaired()
                }
                // Pairing mid-session (e.g. via our connect flow) must move the
                // device from Available to Paired immediately.
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> refreshPaired()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun upsertFound(intent: Intent) {
        val device: BluetoothDevice =
            if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE,
                    BluetoothDevice::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            } ?: return
        val address = device.address ?: return
        if (_paired.value.orEmpty().any { it.address == address }) return
        // Prefer EXTRA_NAME; device.name needs CONNECT and can be null. Never regress
        // a known name to the bare address: repeat ACTION_FOUND sightings (RSSI
        // updates) may omit the name that an earlier broadcast already delivered.
        val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: found[address]?.name
            ?: address
        found[address] = BluetoothDeviceItem(address, name, bonded = false)
        publishAvailable()
    }

    fun isSupported(): Boolean = adapter != null

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasScanPermission(): Boolean {
        val locationOk = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED && locationOk
        } else {
            locationOk
        }
    }

    fun isLocationEnabled(): Boolean {
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= 28) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    fun locationSettingsIntent(): Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    fun register() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        // System BT stack delivers ACTION_FOUND; EXPORTED is required on some OEMs.
        ContextCompat.registerReceiver(
            appContext,
            discoveryReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        receiverRegistered = true
        refreshAdapterState()
        refreshPaired()
    }

    fun unregister() {
        if (!receiverRegistered) return
        runCatching { appContext.unregisterReceiver(discoveryReceiver) }
        receiverRegistered = false
        stopScan()
    }

    fun refreshAdapterState() {
        val a = adapter
        val current = BluetoothLink.state.value
        when {
            a == null -> BluetoothLink.setState(BluetoothConnectionState.Unsupported)
            !a.isEnabled -> BluetoothLink.setState(BluetoothConnectionState.Off)
            current == BluetoothConnectionState.Connected ||
                current == BluetoothConnectionState.Connecting ||
                current == BluetoothConnectionState.Scanning -> Unit
            else -> BluetoothLink.setState(BluetoothConnectionState.On)
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPaired() {
        if (!hasConnectPermission()) {
            _paired.value = emptyList()
            return
        }
        val bonded = adapter?.bondedDevices.orEmpty().map {
            BluetoothDeviceItem(
                address = it.address,
                name = it.name?.takeIf { n -> n.isNotBlank() } ?: it.address,
                bonded = true,
            )
        }.sortedBy { it.name.lowercase() }
        _paired.value = bonded
        // Purge rows that became paired after they were sighted, then re-filter.
        found.keys.removeAll(bonded.map { it.address }.toSet())
        publishAvailable()
    }

    /**
     * @return false if scan could not start (permissions, location off, or adapter busy).
     */
    @SuppressLint("MissingPermission")
    fun startScan(): Boolean {
        val a = adapter ?: return false
        if (!a.isEnabled || !hasScanPermission()) return false
        if (!isLocationEnabled()) return false
        found.clear()
        _available.value = emptyList()
        if (a.isDiscovering) a.cancelDiscovery()
        BluetoothLink.setState(BluetoothConnectionState.Scanning)
        val started = a.startDiscovery()
        if (!started) {
            BluetoothLink.setState(BluetoothConnectionState.On)
            return false
        }
        // Classic + BLE in parallel; both stop on ACTION_DISCOVERY_FINISHED (~12s).
        runCatching { a.bluetoothLeScanner?.startScan(leScanCallback) }
            .onSuccess { leScanning = true }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun stopLeScan() {
        if (!leScanning) return
        leScanning = false
        runCatching { adapter?.bluetoothLeScanner?.stopScan(leScanCallback) }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        val a = adapter ?: return
        stopLeScan()
        if (hasScanPermission() && a.isDiscovering) a.cancelDiscovery()
        if (BluetoothLink.state.value == BluetoothConnectionState.Scanning) {
            BluetoothLink.setState(
                if (a.isEnabled) BluetoothConnectionState.On else BluetoothConnectionState.Off,
            )
        }
    }

    fun isAdapterEnabled(): Boolean = adapter?.isEnabled == true

    fun enableIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    fun bluetoothSettingsIntent(): Intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)

    /**
     * Apps cannot disable Bluetooth on API 33+. Returns true if disable was accepted.
     */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun disableAdapter(): Boolean {
        val a = adapter ?: return false
        if (!a.isEnabled) return true
        if (!hasConnectPermission()) return false
        return if (Build.VERSION.SDK_INT < 33) {
            runCatching { a.disable() }.getOrDefault(false)
        } else {
            false
        }
    }

    companion object {
        /**
         * Paired devices readvertise under rotated private MACs, and dual-mode devices
         * get sighted twice (classic public MAC + BLE random MAC) — so filtering by
         * address alone leaves ghosts and duplicates.
         *
         * ponytail: name-based identity heuristic — apps cannot resolve BLE private
         * addresses (no IRK access), so a rotated MAC is only recognizable by name.
         * Two distinct in-range devices sharing a name will collapse into one row.
         */
        fun dedupeForDisplay(
            found: Collection<BluetoothDeviceItem>,
            paired: List<BluetoothDeviceItem>,
        ): List<BluetoothDeviceItem> {
            val pairedNames = paired
                .filter { it.name != it.address }
                .map { it.name.lowercase() }
                .toSet()
            val pairedAddresses = paired.map { it.address }.toSet()
            return found
                .filterNot { it.address in pairedAddresses }
                .filterNot { it.name != it.address && it.name.lowercase() in pairedNames }
                .distinctBy { if (it.name == it.address) it.address else it.name.lowercase() }
                .sortedBy { it.name == it.address } // named devices first
        }
    }
}
