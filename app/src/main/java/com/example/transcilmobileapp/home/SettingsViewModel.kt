package com.example.transcilmobileapp.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.transcilmobileapp.R

class SettingsViewModel : ViewModel() {

    private val _notificationsEnabled = MutableLiveData(true)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _toastMessage = MutableLiveData<Int?>()
    val toastMessage: LiveData<Int?> = _toastMessage

    fun onNotificationsToggled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun onLanguage() {
        _toastMessage.value = R.string.settings_item_stub
    }

    fun onChangePassword() {
        _toastMessage.value = R.string.settings_item_stub
    }

    fun onHelpCenter() {
        _toastMessage.value = R.string.settings_item_stub
    }

    fun onTerms() {
        _toastMessage.value = R.string.settings_item_stub
    }

    fun onLogout() {
        _toastMessage.value = R.string.settings_logout_stub
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
