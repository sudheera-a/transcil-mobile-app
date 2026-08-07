/**
 * Persists the last Classic Bluetooth device so we can reconnect after process death.
 */
package com.transcil.rider.bluetooth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class BluetoothDeviceStore(private val prefs: SharedPreferences) {

    fun save(address: String, name: String?) {
        prefs.edit {
            putString(KEY_ADDRESS, address)
            putString(KEY_NAME, name)
        }
    }

    fun lastAddress(): String? = prefs.getString(KEY_ADDRESS, null)

    fun lastName(): String? = prefs.getString(KEY_NAME, null)

    fun clear() {
        prefs.edit {
            remove(KEY_ADDRESS)
            remove(KEY_NAME)
        }
    }

    companion object {
        private const val PREFS = "transcil_bluetooth"
        private const val KEY_ADDRESS = "last_address"
        private const val KEY_NAME = "last_name"

        fun create(context: Context): BluetoothDeviceStore =
            BluetoothDeviceStore(
                context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
            )
    }
}
