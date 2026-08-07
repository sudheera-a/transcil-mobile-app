/**
 * Foreground service owning the Classic Bluetooth RFCOMM (SPP) socket.
 * Keeps the link alive when the UI is gone; restores last device on request.
 */
package com.transcil.rider.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.transcil.rider.R
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class BluetoothConnectionService : Service() {

    private val worker = Executors.newSingleThreadExecutor()
    private val store by lazy { BluetoothDeviceStore.create(this) }

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var gatt: BluetoothGatt? = null
    private var gattWrite: BluetoothGattCharacteristic? = null
    private var connectedAddress: String? = null
    private var connectedName: String? = null
    private val reading = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val address = intent.getStringExtra(EXTRA_ADDRESS)
                val name = intent.getStringExtra(EXTRA_NAME) ?: address
                if (address.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                promoteForeground(connecting = true, name = name)
                worker.execute { connectInternal(address, name) }
            }
            ACTION_RESTORE -> {
                val address = store.lastAddress()
                if (address.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (connectedAddress == address && isLinkLive()) {
                    // Service survived UI death — refresh UI + notification only.
                    BluetoothLink.setState(BluetoothConnectionState.Connected, connectedName)
                    promoteForeground(connecting = false, name = connectedName)
                    return START_STICKY
                }
                val name = store.lastName() ?: address
                promoteForeground(connecting = true, name = name)
                worker.execute { connectInternal(address, name) }
            }
            ACTION_SEND -> {
                val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                if (text.isNotEmpty()) worker.execute { sendInternal(text) }
            }
            ACTION_DISCONNECT -> {
                worker.execute {
                    closeLink(updateState = true)
                    stopForegroundCompat()
                    stopSelf()
                }
            }
            else -> {
                // START_STICKY restart after process death delivers a null intent:
                // reconnect to the last device instead of dying silently.
                val address = store.lastAddress()
                if (intent != null || address.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val name = store.lastName() ?: address
                promoteForeground(connecting = true, name = name)
                worker.execute { connectInternal(address, name) }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        reading.set(false)
        closeLink(updateState = false)
        worker.shutdownNow()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun connectInternal(address: String, name: String?) {
        if (!hasConnectPermission()) {
            BluetoothLink.setState(BluetoothConnectionState.Error)
            BluetoothLink.postToast(getString(R.string.bluetooth_permission_needed))
            stopForegroundCompat()
            stopSelf()
            return
        }

        val adapter =
            (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null || !adapter.isEnabled) {
            BluetoothLink.setState(BluetoothConnectionState.Off)
            stopForegroundCompat()
            stopSelf()
            return
        }

        closeLink(updateState = false)
        BluetoothLink.clearConsole()
        BluetoothLink.setState(BluetoothConnectionState.Connecting, name)

        if (adapter.isDiscovering) {
            runCatching { adapter.cancelDiscovery() }
        }

        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull()
        if (device == null) {
            failConnect("Invalid device address")
            return
        }

        connectedAddress = address
        connectedName = name ?: runCatching { device.name }.getOrNull() ?: address

        val type = runCatching { device.type }.getOrNull()
        // BLE-only: GATT. Classic: SPP. Dual: SPP first, GATT fallback if no serial.
        if (type == BluetoothDevice.DEVICE_TYPE_LE) {
            connectGatt(device)
            return
        }

        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            val bonded = awaitBond(device)
            if (!bonded) {
                failConnect("Pairing failed or cancelled")
                return
            }
        }

        val opened = openSocket(device)
        if (opened != null) {
            socket = opened
            input = opened.inputStream
            output = opened.outputStream
            store.save(address, connectedName)
            BluetoothLink.setState(BluetoothConnectionState.Connected, connectedName)
            promoteForeground(connecting = false, name = connectedName)
            startReadLoop()
            return
        }

        if (type == BluetoothDevice.DEVICE_TYPE_DUAL || type == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
            connectGatt(device)
            return
        }
        failConnect("Device offers no SPP serial service (phones/headsets don't)")
    }

    @SuppressLint("MissingPermission")
    private fun connectGatt(device: BluetoothDevice) {
        gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        if (gatt == null) failConnect("Could not start BLE connection")
    }

    private fun isLinkLive(): Boolean =
        socket?.isConnected == true || gatt != null

    @SuppressLint("MissingPermission")
    private fun openSocket(device: BluetoothDevice): BluetoothSocket? {
        val secure = runCatching {
            device.createRfcommSocketToServiceRecord(SPP_UUID)
        }.getOrNull()
        if (secure != null) {
            try {
                secure.connect()
                return secure
            } catch (_: IOException) {
                runCatching { secure.close() }
            }
        }
        val insecure = runCatching {
            device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
        }.getOrNull() ?: return null
        return try {
            insecure.connect()
            insecure
        } catch (_: IOException) {
            runCatching { insecure.close() }
            null
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    store.save(g.device.address, connectedName)
                    BluetoothLink.setState(BluetoothConnectionState.Connected, connectedName)
                    promoteForeground(connecting = false, name = connectedName)
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasConnected =
                        BluetoothLink.state.value == BluetoothConnectionState.Connected
                    closeLink(updateState = false)
                    if (wasConnected) {
                        BluetoothLink.setState(BluetoothConnectionState.Disconnected)
                        BluetoothLink.postToast(getString(R.string.bluetooth_status_disconnected))
                    } else {
                        BluetoothLink.setState(BluetoothConnectionState.Error)
                        BluetoothLink.postToast("Could not connect over BLE")
                    }
                    stopForegroundCompat()
                    stopSelf()
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Suppress("DEPRECATION")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            // Only touch known UART services. Enabling notifications on arbitrary
            // (often protected) characteristics forces Android to auto-bond — pairing
            // alerts on both devices, then a dropped link when bonding fails.
            // ponytail: add the BMS service UUID here once the vendor doc lands.
            val profile = UART_PROFILES.firstOrNull { g.getService(it.service) != null }
            if (profile == null) {
                BluetoothLink.appendConsole(
                    "No UART service on this device — link stays up, console disabled.",
                )
                return
            }
            val svc = g.getService(profile.service)
            gattWrite = svc.getCharacteristic(profile.write)
            val ch = svc.getCharacteristic(profile.notify) ?: return
            g.setCharacteristicNotification(ch, true)
            ch.getDescriptor(CCC_DESCRIPTOR)?.let { ccc ->
                ccc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                g.writeDescriptor(ccc)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val bytes = characteristic.value ?: return
            BluetoothLink.appendConsole("RX: ${formatPayload(bytes)}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun awaitBond(device: BluetoothDevice): Boolean {
        if (device.bondState == BluetoothDevice.BOND_BONDED) return true
        val latch = CountDownLatch(1)
        var success = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
                val d =
                    if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                if (d?.address != device.address) return
                when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)) {
                    BluetoothDevice.BOND_BONDED -> {
                        success = true
                        latch.countDown()
                    }
                    BluetoothDevice.BOND_NONE -> latch.countDown()
                }
            }
        }
        // Bond updates come from the system BT stack (another process).
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
        val started = runCatching { device.createBond() }.getOrDefault(false)
        if (!started && device.bondState != BluetoothDevice.BOND_BONDED) {
            runCatching { unregisterReceiver(receiver) }
            return false
        }
        latch.await(30, TimeUnit.SECONDS)
        runCatching { unregisterReceiver(receiver) }
        return success || device.bondState == BluetoothDevice.BOND_BONDED
    }

    private fun startReadLoop() {
        if (!reading.compareAndSet(false, true)) return
        Thread({
            val buf = ByteArray(1024)
            try {
                while (reading.get()) {
                    val stream = input ?: break
                    val n = stream.read(buf)
                    if (n < 0) break
                    val line = formatPayload(buf.copyOf(n))
                    if (line.isNotEmpty()) {
                        BluetoothLink.appendConsole("RX: $line")
                    }
                }
            } catch (_: IOException) {
                // socket closed or dropped
            } finally {
                reading.set(false)
                if (BluetoothLink.state.value == BluetoothConnectionState.Connected) {
                    BluetoothLink.setState(BluetoothConnectionState.Disconnected)
                    BluetoothLink.postToast(getString(R.string.bluetooth_status_disconnected))
                    stopForegroundCompat()
                    stopSelf()
                }
            }
        }, "bt-spp-read").start()
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun sendInternal(text: String) {
        val g = gatt
        if (g != null) {
            val ch = gattWrite
            if (ch == null) {
                BluetoothLink.postToast("Device exposes no writable characteristic")
                return
            }
            ch.value = "$text\n".toByteArray(Charsets.UTF_8)
            if (g.writeCharacteristic(ch)) {
                BluetoothLink.appendConsole("TX: $text")
            } else {
                BluetoothLink.postToast(getString(R.string.bluetooth_status_error))
            }
            return
        }
        val out = output
        if (out == null || socket?.isConnected != true) {
            BluetoothLink.postToast(getString(R.string.bluetooth_status_disconnected))
            return
        }
        try {
            out.write("$text\n".toByteArray(Charsets.UTF_8))
            out.flush()
            BluetoothLink.appendConsole("TX: $text")
        } catch (_: IOException) {
            BluetoothLink.setState(BluetoothConnectionState.Error)
            BluetoothLink.postToast(getString(R.string.bluetooth_status_error))
            closeLink(updateState = false)
            stopForegroundCompat()
            stopSelf()
        }
    }

    private fun failConnect(message: String) {
        BluetoothLink.setState(BluetoothConnectionState.Error)
        BluetoothLink.postToast(message)
        closeLink(updateState = false)
        stopForegroundCompat()
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun closeLink(updateState: Boolean) {
        reading.set(false)
        runCatching { input?.close() }
        runCatching { output?.close() }
        runCatching { socket?.close() }
        runCatching { gatt?.close() }
        input = null
        output = null
        socket = null
        gatt = null
        gattWrite = null
        connectedAddress = null
        connectedName = null
        if (updateState) {
            BluetoothLink.setState(BluetoothConnectionState.Disconnected)
        }
    }

    private fun promoteForeground(connecting: Boolean, name: String?) {
        ensureChannel()
        val title = getString(R.string.bluetooth_notification_title)
        val text = if (connecting) {
            getString(R.string.bluetooth_notification_connecting)
        } else {
            getString(R.string.bluetooth_notification_connected, name ?: "device")
        }
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pending = PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.bluetooth_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        const val ACTION_CONNECT = "com.transcil.rider.bluetooth.CONNECT"
        const val ACTION_DISCONNECT = "com.transcil.rider.bluetooth.DISCONNECT"
        const val ACTION_SEND = "com.transcil.rider.bluetooth.SEND"
        const val ACTION_RESTORE = "com.transcil.rider.bluetooth.RESTORE"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_NAME = "name"
        const val EXTRA_TEXT = "text"

        private const val CHANNEL_ID = "bluetooth_link"
        private const val NOTIFICATION_ID = 42
        private val SPP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private val CCC_DESCRIPTOR: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        private data class UartProfile(val service: UUID, val write: UUID, val notify: UUID)

        private fun uuid16(short: String): UUID =
            UUID.fromString("0000$short-0000-1000-8000-00805F9B34FB")

        // Known BLE serial services: Nordic UART, HM-10/TI clones (FFE0/FFE1).
        private val UART_PROFILES = listOf(
            UartProfile(
                service = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"),
                write = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"),
                notify = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"),
            ),
            UartProfile(service = uuid16("FFE0"), write = uuid16("FFE1"), notify = uuid16("FFE1")),
        )

        /** Printable payloads render as text; binary (e.g. BMS `7E..7F` frames) as hex. */
        fun formatPayload(bytes: ByteArray): String {
            val text = String(bytes, Charsets.UTF_8)
            val printable = text.none {
                it == '\uFFFD' || (it < ' ' && it != '\n' && it != '\r' && it != '\t')
            }
            return if (printable) {
                text.trim { it <= ' ' || it == '\u0000' }
            } else {
                bytes.joinToString(" ") { "%02X".format(it) }
            }
        }

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

        fun disconnect(context: Context) {
            context.startService(
                Intent(context, BluetoothConnectionService::class.java).apply {
                    action = ACTION_DISCONNECT
                },
            )
        }

        fun send(context: Context, text: String) {
            context.startService(
                Intent(context, BluetoothConnectionService::class.java).apply {
                    action = ACTION_SEND
                    putExtra(EXTRA_TEXT, text)
                },
            )
        }

        fun tryRestore(context: Context) {
            val address = BluetoothDeviceStore.create(context).lastAddress() ?: return
            ContextCompat.startForegroundService(
                context,
                Intent(context, BluetoothConnectionService::class.java).apply {
                    action = ACTION_RESTORE
                    putExtra(EXTRA_ADDRESS, address)
                },
            )
        }
    }
}
