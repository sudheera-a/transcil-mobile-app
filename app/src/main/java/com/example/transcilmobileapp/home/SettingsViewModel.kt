package com.example.transcilmobileapp.home

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.auth.AuthSession
import kotlinx.coroutines.launch

sealed class SettingsNavEvent {
    data class OpenContent(val page: ContentPage) : SettingsNavEvent()
    data object SignedOut : SettingsNavEvent()
}

class SettingsViewModel : ViewModel() {

    private val _notificationsEnabled = MutableLiveData(true)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _rentalAlerts = MutableLiveData(true)
    val rentalAlerts: LiveData<Boolean> = _rentalAlerts

    private val _batteryAlerts = MutableLiveData(true)
    val batteryAlerts: LiveData<Boolean> = _batteryAlerts

    private val _offerAlerts = MutableLiveData(false)
    val offerAlerts: LiveData<Boolean> = _offerAlerts

    private val _darkTheme = MutableLiveData(false)
    val darkTheme: LiveData<Boolean> = _darkTheme

    private val _toastMessage = MutableLiveData<Int?>()
    val toastMessage: LiveData<Int?> = _toastMessage

    private val _navEvent = MutableLiveData<SettingsNavEvent?>()
    val navEvent: LiveData<SettingsNavEvent?> = _navEvent

    @Volatile private var signingOut = false

    fun onNotificationsToggled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        _rentalAlerts.value = enabled
        _batteryAlerts.value = enabled
    }

    fun onRentalAlerts(enabled: Boolean) {
        _rentalAlerts.value = enabled
        syncMasterToggle()
    }

    fun onBatteryAlerts(enabled: Boolean) {
        _batteryAlerts.value = enabled
        syncMasterToggle()
    }

    fun onOfferAlerts(enabled: Boolean) {
        _offerAlerts.value = enabled
    }

    fun onDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO,
        )
    }

    private fun syncMasterToggle() {
        _notificationsEnabled.value =
            (_rentalAlerts.value == true) || (_batteryAlerts.value == true)
    }

    fun onLanguage() {
        _toastMessage.value = R.string.settings_item_stub
    }

    fun onChangePassword() {
        _toastMessage.value = R.string.settings_item_stub
    }

    fun onDeleteAccount() {
        _toastMessage.value = R.string.settings_item_stub
    }

    fun onHelpCenter() {
        _navEvent.value = SettingsNavEvent.OpenContent(ContentPage.HELP)
    }

    fun onTerms() {
        _navEvent.value = SettingsNavEvent.OpenContent(ContentPage.TERMS_PRIVACY)
    }

    fun onLogout() {
        if (signingOut) return
        signingOut = true
        viewModelScope.launch {
            AuthSession.signOut()
            _navEvent.value = SettingsNavEvent.SignedOut
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearNavEvent() {
        _navEvent.value = null
    }
}
