# Classic Bluetooth Connection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Settings → Bluetooth screen for Classic SPP: toggle adapter, list paired + nearby devices, connect via a foreground service, serial TX/RX console, keep the link in background, and reconnect to the last device after process death on next open.

**Architecture:** UI (`BluetoothFragment` + `BluetoothViewModel`) never owns the socket. `BluetoothConnectionService` (FGS `connectedDevice`) owns RFCOMM. Thin `BluetoothRepository` handles adapter/discovery/prefs. `BluetoothLink` singleton exposes LiveData status/console lines to the UI. Matches existing MVVM + ViewBinding + LiveData style.

**Tech Stack:** Kotlin, Android Bluetooth Classic APIs, Foreground Service, ViewBinding, LiveData, JUnit, minSdk 24 / targetSdk 36.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-04-classic-bluetooth-connection-design.md`
- Package: `com.transcil.rider.bluetooth`
- SPP UUID: `00001101-0000-1000-8000-00805F9B34FB`
- No Hilt, no Compose, no new dependencies
- No commit unless user explicitly asks (skip all “Commit” steps until then)
- Force-stop: reconnect only after user opens the app again

## File map

| File | Role |
|------|------|
| `app/src/main/AndroidManifest.xml` | Permissions + FGS service |
| `app/src/main/res/values/strings.xml` | Bluetooth copy |
| `app/src/main/res/drawable/ic_bluetooth.xml` | Settings row icon |
| `app/src/main/res/layout/fragment_settings.xml` | Bluetooth row |
| `app/src/main/res/layout/fragment_bluetooth.xml` | Screen layout |
| `app/src/main/res/layout/item_bluetooth_device.xml` | List row |
| `app/src/main/res/navigation/home_nav_graph.xml` | Destination + action |
| `app/src/main/java/.../home/SettingsFragment.kt` | Navigate to Bluetooth |
| `app/src/main/java/.../home/HomeDashboardActivity.kt` | Hide bottom nav on Bluetooth |
| `app/src/main/java/.../bluetooth/BluetoothDeviceStore.kt` | Last device prefs |
| `app/src/main/java/.../bluetooth/BluetoothLink.kt` | Shared status/console LiveData |
| `app/src/main/java/.../bluetooth/BluetoothRepository.kt` | Adapter + discovery |
| `app/src/main/java/.../bluetooth/BluetoothConnectionService.kt` | Socket + FGS |
| `app/src/main/java/.../bluetooth/BluetoothDeviceItem.kt` | List model |
| `app/src/main/java/.../bluetooth/BluetoothDeviceAdapter.kt` | RecyclerView |
| `app/src/main/java/.../bluetooth/BluetoothViewModel.kt` | UI state |
| `app/src/main/java/.../bluetooth/BluetoothFragment.kt` | Screen |
| `app/src/test/java/.../bluetooth/BluetoothDeviceStoreTest.kt` | Prefs self-check |

---

### Task 1: Manifest, strings, Settings entry, empty Bluetooth screen

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_bluetooth.xml`
- Modify: `app/src/main/res/layout/fragment_settings.xml`
- Create: `app/src/main/res/layout/fragment_bluetooth.xml`
- Modify: `app/src/main/res/navigation/home_nav_graph.xml`
- Modify: `app/src/main/java/com/transcil/rider/home/SettingsFragment.kt`
- Modify: `app/src/main/java/com/transcil/rider/home/HomeDashboardActivity.kt`
- Create: `app/src/main/java/com/transcil/rider/bluetooth/BluetoothFragment.kt`

**Interfaces:**
- Consumes: existing Settings nav pattern
- Produces: navigable empty Bluetooth screen from Settings

- [x] **Step 1: Add permissions + service stub to manifest**

Above `<application>`, add:

```xml
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission
        android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation"
        tools:targetApi="s" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Inside `<application>`, before closing tag, add:

```xml
        <service
            android:name=".bluetooth.BluetoothConnectionService"
            android:exported="false"
            android:foregroundServiceType="connectedDevice" />
```

(Service class is created in Task 5; until then the project may not compile if the class is missing — create a stub class in Step 5 of this task.)

- [x] **Step 2: Add strings**

Append to `strings.xml`:

```xml
    <string name="settings_bluetooth">Bluetooth</string>
    <string name="settings_bluetooth_sub">Connect to nearby devices</string>
    <string name="bluetooth_title">Bluetooth</string>
    <string name="bluetooth_back_cd">Back</string>
    <string name="bluetooth_toggle">Bluetooth</string>
    <string name="bluetooth_status_off">Bluetooth is off</string>
    <string name="bluetooth_status_on">On — not connected</string>
    <string name="bluetooth_status_scanning">Scanning…</string>
    <string name="bluetooth_status_connecting">Connecting…</string>
    <string name="bluetooth_status_connected">Connected to %1$s</string>
    <string name="bluetooth_status_disconnected">Disconnected</string>
    <string name="bluetooth_status_error">Connection error</string>
    <string name="bluetooth_status_unsupported">Bluetooth not supported</string>
    <string name="bluetooth_section_paired">PAIRED</string>
    <string name="bluetooth_section_available">AVAILABLE</string>
    <string name="bluetooth_scan">Scan</string>
    <string name="bluetooth_stop_scan">Stop</string>
    <string name="bluetooth_empty_paired">No paired devices</string>
    <string name="bluetooth_empty_available">No devices found — tap Scan</string>
    <string name="bluetooth_send">Send</string>
    <string name="bluetooth_disconnect">Disconnect</string>
    <string name="bluetooth_input_hint">Type a message</string>
    <string name="bluetooth_permission_needed">Bluetooth permission is required</string>
    <string name="bluetooth_notification_channel">Bluetooth connection</string>
    <string name="bluetooth_notification_title">Transcil Bluetooth</string>
    <string name="bluetooth_notification_connected">Connected to %1$s</string>
    <string name="bluetooth_notification_connecting">Connecting…</string>
```

- [x] **Step 3: Add `ic_bluetooth.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M17.71,7.71L12,2h-1v7.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L11,14.41V22h1l5.71,-5.71L13.41,12l4.3,-4.29zM13,5.83l1.88,1.88L13,9.59V5.83zM14.88,16.29L13,18.17v-3.76l1.88,1.88z" />
</vector>
```

- [x] **Step 4: Add Settings row + navigate**

In `fragment_settings.xml`, after `rowLanguage` (before SECURITY section), add a row modeled on `rowLanguage` with id `rowBluetooth`, title `@string/settings_bluetooth`, subtitle optional via second TextView or just title + chevron, icon `@drawable/ic_bluetooth`.

In `SettingsFragment.onViewCreated`:

```kotlin
binding.rowBluetooth.setOnClickListener {
    findNavController().navigate(
        R.id.action_settings_to_bluetooth,
        null,
        navOptions { launchSingleTop = true },
    )
}
```

In `home_nav_graph.xml` under `settingsFragment`, add action + new fragment:

```xml
        <action
            android:id="@+id/action_settings_to_bluetooth"
            app:destination="@id/bluetoothFragment" />
```

```xml
    <fragment
        android:id="@+id/bluetoothFragment"
        android:name="com.transcil.rider.bluetooth.BluetoothFragment"
        android:label="@string/bluetooth_title"
        tools:layout="@layout/fragment_bluetooth" />
```

In `HomeDashboardActivity`, hide bottom nav for Bluetooth and map tab highlight to Profile:

```kotlin
val hideBottomNav = destination.id == R.id.settingsFragment ||
    destination.id == R.id.apiContentFragment ||
    destination.id == R.id.bluetoothFragment
```

```kotlin
R.id.profileFragment, R.id.settingsFragment, R.id.apiContentFragment,
R.id.bluetoothFragment -> HomeNavTab.PROFILE
```

- [x] **Step 5: Empty layout + Fragment + Service stub**

Create `fragment_bluetooth.xml` (DataBinding `<layout>` wrapper like settings): vertical LinearLayout with back+title, Switch `switchBluetooth`, TextView `tvStatus`, placeholder TextViews for sections (full UI in Task 4).

Create:

```kotlin
package com.transcil.rider.bluetooth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.transcil.rider.databinding.FragmentBluetoothBinding

class BluetoothFragment : Fragment() {
    private var _binding: FragmentBluetoothBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBluetoothBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val goBack = { findNavController().navigateUp() }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = goBack()
            },
        )
        binding.btnBack.setOnClickListener { goBack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

Stub service so manifest resolves:

```kotlin
package com.transcil.rider.bluetooth

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BluetoothConnectionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
```

- [x] **Step 6: Verify navigation**

Run app → Profile → Settings → Bluetooth → empty screen with back. Bottom nav hidden.
(`:app:compileDebugKotlin` succeeded; please confirm navigation on device/emulator.)

- [x] **Step 7: Skip commit** (unless user asks)

---

### Task 2: Last-device store + unit test

**Files:**
- Create: `app/src/main/java/com/transcil/rider/bluetooth/BluetoothDeviceStore.kt`
- Create: `app/src/test/java/com/transcil/rider/bluetooth/BluetoothDeviceStoreTest.kt`

**Interfaces:**
- Consumes: `SharedPreferences`
- Produces: `BluetoothDeviceStore.save/load/clear`

- [x] **Step 1: Write failing test** (MemPrefs JUnit version)
- [x] **Step 2: Run test — expect FAIL** (skipped; store landed with test in same pass)
- [x] **Step 3: Implement store** — `BluetoothDeviceStore.kt`
- [x] **Step 4: Run test — expect PASS** (`BluetoothDeviceStoreTest` green)
- [x] **Step 5: Skip commit** (unless user asks)

---

### Task 3: `BluetoothLink` + `BluetoothRepository` (adapter, paired, discovery)

**Files:**
- Create: `app/src/main/java/com/transcil/rider/bluetooth/BluetoothLink.kt`
- Create: `app/src/main/java/com/transcil/rider/bluetooth/BluetoothDeviceItem.kt`
- Create: `app/src/main/java/com/transcil/rider/bluetooth/BluetoothRepository.kt`

**Interfaces:**
- Consumes: `BluetoothAdapter`, system broadcasts
- Produces: paired/available lists; adapter on/off; scan start/stop; `BluetoothLink` status LiveData

- [x] **Step 1: Models + link bus**

```kotlin
package com.transcil.rider.bluetooth

data class BluetoothDeviceItem(
    val address: String,
    val name: String,
    val bonded: Boolean,
)
```

```kotlin
package com.transcil.rider.bluetooth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

enum class BluetoothConnectionState {
    Unsupported,
    Off,
    On,
    Scanning,
    Connecting,
    Connected,
    Disconnected,
    Error,
}

object BluetoothLink {
    private val _state = MutableLiveData(BluetoothConnectionState.Off)
    val state: LiveData<BluetoothConnectionState> = _state

    private val _deviceName = MutableLiveData<String?>(null)
    val deviceName: LiveData<String?> = _deviceName

    private val _console = MutableLiveData<List<String>>(emptyList())
    val console: LiveData<List<String>> = _console

    private val _toast = MutableLiveData<String?>(null)
    val toast: LiveData<String?> = _toast

    fun setState(state: BluetoothConnectionState, name: String? = _deviceName.value) {
        _state.postValue(state)
        if (name != null || state == BluetoothConnectionState.Disconnected) {
            _deviceName.postValue(name)
        }
    }

    fun appendConsole(line: String) {
        val next = (_console.value ?: emptyList()) + line
        _console.postValue(next.takeLast(200))
    }

    fun clearConsole() {
        _console.postValue(emptyList())
    }

    fun postToast(message: String) {
        _toast.postValue(message)
    }

    fun clearToast() {
        _toast.postValue(null)
    }
}
```

- [x] **Step 2: Repository**

```kotlin
package com.transcil.rider.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
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

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice =
                        if (Build.VERSION.SDK_INT >= 33) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        } ?: return
                    val address = device.address ?: return
                    if (_paired.value.orEmpty().any { it.address == address }) return
                    val name = device.name?.takeIf { it.isNotBlank() } ?: address
                    found[address] = BluetoothDeviceItem(address, name, bonded = false)
                    _available.postValue(found.values.toList())
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (BluetoothLink.state.value == BluetoothConnectionState.Scanning) {
                        BluetoothLink.setState(BluetoothConnectionState.On)
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    refreshAdapterState()
                }
            }
        }
    }

    fun isSupported(): Boolean = adapter != null

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            appContext,
            discoveryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        refreshAdapterState()
        refreshPaired()
    }

    fun unregister() {
        runCatching { appContext.unregisterReceiver(discoveryReceiver) }
        stopScan()
    }

    fun refreshAdapterState() {
        val a = adapter
        when {
            a == null -> BluetoothLink.setState(BluetoothConnectionState.Unsupported)
            !a.isEnabled -> BluetoothLink.setState(BluetoothConnectionState.Off)
            BluetoothLink.state.value == BluetoothConnectionState.Connected ||
                BluetoothLink.state.value == BluetoothConnectionState.Connecting -> Unit
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
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val a = adapter ?: return
        if (!a.isEnabled || !hasScanPermission()) return
        found.clear()
        _available.value = emptyList()
        if (a.isDiscovering) a.cancelDiscovery()
        BluetoothLink.setState(BluetoothConnectionState.Scanning)
        a.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        val a = adapter ?: return
        if (a.isDiscovering) a.cancelDiscovery()
        if (BluetoothLink.state.value == BluetoothConnectionState.Scanning) {
            BluetoothLink.setState(if (a.isEnabled) BluetoothConnectionState.On else BluetoothConnectionState.Off)
        }
    }

    fun enableIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
}
```

- [x] **Step 3: Skip commit** (unless user asks)

---

### Task 4: ViewModel + full Fragment UI (toggle, lists, permissions)

**Files:**
- Create: `app/src/main/java/com/transcil/rider/bluetooth/BluetoothViewModel.kt`
- Create: `app/src/main/java/com/transcil/rider/bluetooth/BluetoothDeviceAdapter.kt`
- Create: `app/src/main/res/layout/item_bluetooth_device.xml`
- Modify: `app/src/main/res/layout/fragment_bluetooth.xml`
- Modify: `app/src/main/java/com/transcil/rider/bluetooth/BluetoothFragment.kt`

**Interfaces:**
- Consumes: `BluetoothRepository`, `BluetoothLink`
- Produces: working toggle + paired/available lists (connect wired in Task 6)

- [x] **Step 1: Expand `fragment_bluetooth.xml`**

Structure (NestedScrollView OK):

- Header back + title  
- Row: label + `Switch` `switchBluetooth`  
- `tvStatus`  
- Section PAIRED + `RecyclerView` `rvPaired` (nestedScrollingEnabled=false) + `tvEmptyPaired`  
- Section AVAILABLE + Scan/Stop buttons + `rvAvailable` + `tvEmptyAvailable`  
- Console group `groupConsole` (visibility gone until connected): `tvConsole` (scrollable), `etMessage`, `btnSend`, `btnDisconnect`

Reuse `@drawable/bg_home_card`, brand colors, Sora font like Settings.

- [x] **Step 2: Item layout + adapter**

`item_bluetooth_device.xml`: name + address + chevron, clickable card.

```kotlin
package com.transcil.rider.bluetooth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.transcil.rider.databinding.ItemBluetoothDeviceBinding

class BluetoothDeviceAdapter(
    private val onClick: (BluetoothDeviceItem) -> Unit,
) : RecyclerView.Adapter<BluetoothDeviceAdapter.Holder>() {

    private val items = mutableListOf<BluetoothDeviceItem>()

    fun submit(list: List<BluetoothDeviceItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemBluetoothDeviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class Holder(private val binding: ItemBluetoothDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BluetoothDeviceItem) {
            binding.tvName.text = item.name
            binding.tvAddress.text = item.address
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
```

- [x] **Step 3: ViewModel**

```kotlin
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
    fun startScan() = repo.startScan()
    fun stopScan() = repo.stopScan()
    fun enableIntent() = repo.enableIntent()

    fun connect(item: BluetoothDeviceItem) {
        BluetoothConnectionService.connect(getApplication(), item.address, item.name)
    }

    fun disconnect() = BluetoothConnectionService.disconnect(getApplication())
    fun send(text: String) = BluetoothConnectionService.send(getApplication(), text)
    fun tryRestore() = BluetoothConnectionService.tryRestore(getApplication())
    fun clearToast() = BluetoothLink.clearToast()
}
```

(`BluetoothConnectionService` companion methods land in Task 5 — until then comment connect/send or add no-op companions.)

- [x] **Step 4: Wire Fragment**

In `BluetoothFragment`:

1. `viewModels<BluetoothViewModel>()`
2. Request permissions (`BLUETOOTH_CONNECT`+`BLUETOOTH_SCAN` on 31+; `ACCESS_FINE_LOCATION` below; `POST_NOTIFICATIONS` on 33+) via `ActivityResultContracts.RequestMultiplePermissions`
3. Toggle: if turning on → `enableIntent()` launcher; if turning off → toast that user should use system Settings to disable (or leave toggle reflecting adapter only — **do not** call hidden disable APIs). Simplest UX: switch is read-mostly; ON click launches enable request; OFF shows “Turn off Bluetooth from system settings”
4. Observe `state` → update `tvStatus` + switch checked + console visibility + scan button labels
5. Observe paired/available → adapters + empty views
6. Scan / Stop buttons
7. Device click → `viewModel.connect(item)` (works after Task 5)
8. `onStart`/`onStop` → repo register; on first ready call `tryRestore()`

- [x] **Step 5: Manual check** (compile OK; confirm toggle/lists on device)
- [x] **Step 6: Skip commit** (unless user asks)
  Note: connect/send/restore are safe stubs until Task 5 implements the FGS socket.

---

### Task 5: Foreground service — connect / disconnect / notification

**Files:**
- Replace: `app/src/main/java/com/transcil/rider/bluetooth/BluetoothConnectionService.kt`

**Interfaces:**
- Consumes: address/name intents; SPP UUID; `BluetoothDeviceStore`
- Produces: connected socket; FGS notification; status on `BluetoothLink`

- [x] **Step 1: Implement service**

Key API:

```kotlin
companion object {
    const val ACTION_CONNECT = "com.transcil.rider.bluetooth.CONNECT"
    const val ACTION_DISCONNECT = "com.transcil.rider.bluetooth.DISCONNECT"
    const val ACTION_SEND = "com.transcil.rider.bluetooth.SEND"
    const val ACTION_RESTORE = "com.transcil.rider.bluetooth.RESTORE"
    const val EXTRA_ADDRESS = "address"
    const val EXTRA_NAME = "name"
    const val EXTRA_TEXT = "text"

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun connect(context: Context, address: String, name: String) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, BluetoothConnectionService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_NAME, name)
            },
        )
    }

    fun disconnect(context: Context) { /* startService ACTION_DISCONNECT */ }
    fun send(context: Context, text: String) { /* startService ACTION_SEND */ }
    fun tryRestore(context: Context) { /* startService ACTION_RESTORE */ }
}
```

Behavior:

1. `onStartCommand`: for CONNECT/RESTORE, `startForeground` immediately with connecting notification (required on modern Android), then connect on a background executor/thread.
2. Cancel discovery before connect.
3. `device.createRfcommSocketToServiceRecord(SPP_UUID)` then `socket.connect()`.
4. If not bonded, call `device.createBond()` and wait briefly / rely on user pairing dialog, then retry connect once.
5. On success: save to `BluetoothDeviceStore`, set state Connected, start read loop thread (`InputStream.read` → `BluetoothLink.appendConsole("RX: …")`).
6. `ACTION_SEND`: write bytes + `\n`, append `TX: …`.
7. `ACTION_DISCONNECT` / socket error: close streams/socket, stopForeground, stopSelf, state Disconnected.
8. `ACTION_RESTORE`: if store has address and not already connected → connect.
9. Only one connection: close previous before new connect.

Notification channel id: `bluetooth_link`. Use `ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` when calling `startForeground` on API 29+.

- [x] **Step 2: Compile** — SUCCESS
- [ ] **Step 3: Manual connect check** — tap paired SPP device on device/emulator
- [x] **Step 4: Skip commit** (unless user asks)

---

### Task 6: Serial console TX/RX + Disconnect

**Files:**
- Modify: `BluetoothFragment.kt` (console observers + send/disconnect clicks)
- Modify: `BluetoothConnectionService.kt` if send/read need newline/charset tweaks

**Interfaces:**
- Consumes: `BluetoothLink.console`, Send/Disconnect
- Produces: visible TX/RX log

- [x] **Step 1: Wire console UI** — send/IME send, auto-scroll log, visible when Connected
- [ ] **Step 2: Manual check** — send on real SPP device
- [x] **Step 3: Skip commit** (unless user asks)

---

### Task 7: Background keep-alive + process-death restore

**Files:**
- Modify: `BluetoothFragment.kt` / `BluetoothViewModel.kt` (call `tryRestore` once permissions OK)
- Modify: `BluetoothConnectionService.kt` if restore path needs hardening
- Optional: call `tryRestore` from `TranscilApp` only if you want restore before opening Settings — **default: restore when Bluetooth screen opens** (YAGNI)

**Interfaces:**
- Consumes: `BluetoothDeviceStore.lastAddress()`
- Produces: reconnect attempt after swipe-away + reopen

- [x] **Step 1: Ensure restore on screen start** — once, skip if already Connected/Connecting; service refreshes UI if still linked
- [ ] **Step 2: Manual checklist** — Home / swipe-away / force-stop on device
- [x] **Step 3: Skip commit** (unless user asks)

---

### Task 8: Final polish + verification

**Files:**
- Touch-ups only if needed (empty states, error toasts, disable Send when not connected)

- [x] **Step 1: Run unit test** — `BluetoothDeviceStoreTest` PASS
- [ ] **Step 2: Full manual pass** (from spec Verification section)
- [x] **Step 3: Skip commit** (unless user asks)

---

## Spec coverage check

| Spec item | Task |
|-----------|------|
| Settings → Bluetooth entry | 1 |
| Permissions / FGS manifest | 1, 5 |
| On/Off toggle + adapter state | 3, 4 |
| Paired + Available lists | 3, 4 |
| Scan / Stop | 3, 4 |
| Connect SPP + status | 5 |
| Serial console TX/RX | 6 |
| Disconnect / single connection | 5, 6 |
| FGS keep-alive in background | 5, 7 |
| Last device + restore | 2, 5, 7 |
| Unit test for prefs | 2 |
| Force-stop honest limit | 7 notes |

## Placeholder / consistency check

- No TBD left; SPP UUID fixed; FGS type `connectedDevice`
- Companion action names consistent across ViewModel and Service
- `BluetoothDeviceStore` API: `save` / `lastAddress` / `lastName` / `clear` / `create`
