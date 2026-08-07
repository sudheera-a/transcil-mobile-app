/**
 * Shared bus between [BluetoothConnectionService] and the Bluetooth UI.
 * Service posts connection/console updates; Fragment/ViewModel observes LiveData.
 */
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
        when (state) {
            BluetoothConnectionState.Disconnected,
            BluetoothConnectionState.Off,
            BluetoothConnectionState.Unsupported,
            -> _deviceName.postValue(null)
            else -> if (name != null) _deviceName.postValue(name)
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
