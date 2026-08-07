/**
 * Settings → Bluetooth: adapter toggle, paired/available lists, serial console.
 */
package com.transcil.rider.bluetooth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.transcil.rider.R
import com.transcil.rider.databinding.FragmentBluetoothBinding

class BluetoothFragment : Fragment() {

    private var _binding: FragmentBluetoothBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BluetoothViewModel by viewModels()

    private lateinit var pairedAdapter: BluetoothDeviceAdapter
    private lateinit var availableAdapter: BluetoothDeviceAdapter

    private var ignoreSwitchCallback = false
    private var restoreAttempted = false
    private var pendingConnect: BluetoothDeviceItem? = null
    private var pendingRestore = false

    private val enableBluetooth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // User may cancel the system enable dialog — resync switch to real adapter state.
        viewModel.refreshAdapterState()
        viewModel.refreshPaired()
        syncSwitchToAdapter()
    }

    private val bluetoothSettings = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshAdapterState()
        viewModel.refreshPaired()
        syncSwitchToAdapter()
    }

    private val locationSettings = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // User may have enabled Location — retry is manual via Scan.
    }

    private val requestBtPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = bluetoothPermissions().all { perm ->
            result[perm] == true || isGranted(perm)
        }
        if (!granted) {
            Toast.makeText(
                requireContext(),
                R.string.bluetooth_permission_needed,
                Toast.LENGTH_SHORT,
            ).show()
            return@registerForActivityResult
        }
        onBluetoothPermissionsReady()
    }

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Denied is OK for listing/scan; FGS notification may be silent on API 33+.
        pendingConnect?.let { item ->
            pendingConnect = null
            viewModel.connect(item)
        }
        if (pendingRestore) {
            pendingRestore = false
            tryRestoreOnce()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBluetoothBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val goBack = { findNavController().navigateUp() }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goBack()
                }
            },
        )
        binding.btnBack.setOnClickListener { goBack() }

        pairedAdapter = BluetoothDeviceAdapter { onDeviceClicked(it) }
        availableAdapter = BluetoothDeviceAdapter { onDeviceClicked(it) }
        binding.rvPaired.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAvailable.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPaired.adapter = pairedAdapter
        binding.rvAvailable.adapter = availableAdapter

        binding.switchBluetooth.setOnCheckedChangeListener { _, checked ->
            if (ignoreSwitchCallback) return@setOnCheckedChangeListener
            if (checked) {
                if (viewModel.isAdapterEnabled()) {
                    syncSwitchToAdapter()
                    return@setOnCheckedChangeListener
                }
                if (!ensureBluetoothPermissions()) {
                    syncSwitchToAdapter()
                    return@setOnCheckedChangeListener
                }
                enableBluetooth.launch(viewModel.enableIntent())
            } else {
                if (!viewModel.isAdapterEnabled()) {
                    syncSwitchToAdapter()
                    return@setOnCheckedChangeListener
                }
                if (!ensureBluetoothPermissions()) {
                    syncSwitchToAdapter()
                    return@setOnCheckedChangeListener
                }
                // API 33+: apps cannot disable Bluetooth; open system settings instead.
                val disabled = viewModel.disableAdapter()
                if (!disabled) {
                    Toast.makeText(
                        requireContext(),
                        R.string.bluetooth_turn_off_system,
                        Toast.LENGTH_SHORT,
                    ).show()
                    bluetoothSettings.launch(viewModel.bluetoothSettingsIntent())
                }
                viewModel.refreshAdapterState()
                syncSwitchToAdapter()
            }
        }

        binding.btnScan.setOnClickListener {
            if (!ensureBluetoothPermissions()) return@setOnClickListener
            if (viewModel.state.value == BluetoothConnectionState.Scanning) {
                viewModel.stopScan()
                return@setOnClickListener
            }
            if (!viewModel.isLocationEnabled()) {
                Toast.makeText(
                    requireContext(),
                    R.string.bluetooth_location_required,
                    Toast.LENGTH_LONG,
                ).show()
                locationSettings.launch(viewModel.locationSettingsIntent())
                return@setOnClickListener
            }
            if (!viewModel.startScan()) {
                Toast.makeText(
                    requireContext(),
                    R.string.bluetooth_scan_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        binding.tvConsole.movementMethod = ScrollingMovementMethod.getInstance()
        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
        binding.btnDisconnect.setOnClickListener { viewModel.disconnect() }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state, viewModel.deviceName.value)
        }
        viewModel.deviceName.observe(viewLifecycleOwner) { name ->
            renderState(viewModel.state.value, name)
        }
        viewModel.paired.observe(viewLifecycleOwner) { list ->
            pairedAdapter.submit(list.orEmpty())
            binding.tvEmptyPaired.isVisible = list.isNullOrEmpty()
        }
        viewModel.available.observe(viewLifecycleOwner) { list ->
            availableAdapter.submit(list.orEmpty())
            updateAvailableEmpty(list.isNullOrEmpty())
        }
        viewModel.console.observe(viewLifecycleOwner) { lines ->
            binding.tvConsole.text = lines.orEmpty().joinToString("\n")
            binding.tvConsole.post {
                val layout = binding.tvConsole.layout ?: return@post
                val scrollAmount =
                    layout.getLineTop(binding.tvConsole.lineCount) - binding.tvConsole.height
                if (scrollAmount > 0) binding.tvConsole.scrollTo(0, scrollAmount)
            }
        }
        viewModel.toast.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }

        if (!viewModel.isSupported()) {
            binding.tvStatus.setText(R.string.bluetooth_status_unsupported)
            binding.switchBluetooth.isEnabled = false
            binding.btnScan.isEnabled = false
            return
        }

        if (hasBluetoothPermissions()) {
            onBluetoothPermissionsReady()
        } else {
            requestBtPermissions.launch(bluetoothPermissions())
        }
    }

    override fun onStop() {
        viewModel.onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onDeviceClicked(item: BluetoothDeviceItem) {
        if (!ensureBluetoothPermissions()) return
        if (needsNotificationPermission() && !isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
            pendingConnect = item
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        viewModel.connect(item)
    }

    private fun onBluetoothPermissionsReady() {
        viewModel.onStart()
        viewModel.refreshPaired()
        if (restoreAttempted) return
        if (needsNotificationPermission() && !isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
            pendingRestore = true
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        tryRestoreOnce()
    }

    private fun tryRestoreOnce() {
        if (restoreAttempted) return
        restoreAttempted = true
        val s = viewModel.state.value
        if (s != BluetoothConnectionState.Connected &&
            s != BluetoothConnectionState.Connecting
        ) {
            viewModel.tryRestore()
        }
    }

    private fun sendMessage() {
        if (viewModel.state.value != BluetoothConnectionState.Connected) return
        val text = binding.etMessage.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        viewModel.send(text)
        binding.etMessage.text = null
    }

    private fun renderState(state: BluetoothConnectionState?, name: String?) {
        val s = state ?: BluetoothConnectionState.Off
        val statusText = when (s) {
            BluetoothConnectionState.Unsupported -> getString(R.string.bluetooth_status_unsupported)
            BluetoothConnectionState.Off -> getString(R.string.bluetooth_status_off)
            BluetoothConnectionState.On -> getString(R.string.bluetooth_status_on)
            BluetoothConnectionState.Scanning -> getString(R.string.bluetooth_status_scanning)
            BluetoothConnectionState.Connecting -> getString(R.string.bluetooth_status_connecting)
            BluetoothConnectionState.Connected ->
                getString(R.string.bluetooth_status_connected, name ?: "device")
            BluetoothConnectionState.Disconnected -> getString(R.string.bluetooth_status_disconnected)
            BluetoothConnectionState.Error -> getString(R.string.bluetooth_status_error)
        }
        binding.tvStatus.text = statusText

        // Switch mirrors the real adapter, not connection status text.
        syncSwitchToAdapter()

        binding.btnScan.text = getString(
            if (s == BluetoothConnectionState.Scanning) {
                R.string.bluetooth_stop_scan
            } else {
                R.string.bluetooth_scan
            },
        )
        updateAvailableEmpty(viewModel.available.value.isNullOrEmpty())
        binding.groupConsole.isVisible = s == BluetoothConnectionState.Connected
        binding.btnSend.isEnabled = s == BluetoothConnectionState.Connected
    }

    private fun updateAvailableEmpty(isEmpty: Boolean) {
        binding.tvEmptyAvailable.isVisible = isEmpty
        if (!isEmpty) return
        binding.tvEmptyAvailable.setText(
            if (viewModel.state.value == BluetoothConnectionState.Scanning) {
                R.string.bluetooth_empty_scanning
            } else {
                R.string.bluetooth_empty_available
            },
        )
    }

    private fun syncSwitchToAdapter() {
        val on = viewModel.isAdapterEnabled()
        ignoreSwitchCallback = true
        binding.switchBluetooth.isChecked = on
        ignoreSwitchCallback = false
    }

    private fun ensureBluetoothPermissions(): Boolean {
        if (hasBluetoothPermissions()) return true
        requestBtPermissions.launch(bluetoothPermissions())
        return false
    }

    private fun hasBluetoothPermissions(): Boolean =
        bluetoothPermissions().all { isGranted(it) }

    private fun bluetoothPermissions(): Array<String> {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 31) {
            perms += Manifest.permission.BLUETOOTH_CONNECT
            perms += Manifest.permission.BLUETOOTH_SCAN
        }
        return perms.toTypedArray()
    }

    private fun needsNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= 33

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), permission) ==
            PackageManager.PERMISSION_GRANTED
}
