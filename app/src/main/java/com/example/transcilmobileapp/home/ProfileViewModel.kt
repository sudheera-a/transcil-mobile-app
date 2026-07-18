package com.example.transcilmobileapp.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.kyc.KycProgressRepository

sealed class ProfileNavEvent {
    data object OpenSettings : ProfileNavEvent()
    data object OpenDocuments : ProfileNavEvent()
    data class ShowStub(val titleRes: Int) : ProfileNavEvent()
    data object Logout : ProfileNavEvent()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val _displayName = MutableLiveData("")
    val displayName: LiveData<String> = _displayName

    private val _riderId = MutableLiveData("")
    val riderId: LiveData<String> = _riderId

    private val _phone = MutableLiveData(ProfileDisplayFormatter.EMPTY)
    val phone: LiveData<String> = _phone

    private val _email = MutableLiveData(ProfileDisplayFormatter.EMPTY)
    val email: LiveData<String> = _email

    private val _location = MutableLiveData(ProfileDisplayFormatter.EMPTY)
    val location: LiveData<String> = _location

    private val _kycStatus = MutableLiveData(KycStatus.PENDING)
    val kycStatus: LiveData<KycStatus> = _kycStatus

    private val _menuItems = MutableLiveData(ProfileRepository.menuItems(kycApproved = false))
    val menuItems: LiveData<List<ProfileMenuItem>> = _menuItems

    private val _navEvent = MutableLiveData<ProfileNavEvent?>()
    val navEvent: LiveData<ProfileNavEvent?> = _navEvent

    private val _toastMessage = MutableLiveData<Int?>()
    val toastMessage: LiveData<Int?> = _toastMessage

    fun bind(status: KycStatus) {
        _kycStatus.value = status
        val personal = KycProgressRepository.personalDraft()
        val address = KycProgressRepository.addressDraft()

        val draftName = personal.fullName.trim()
        _displayName.value = draftName.ifBlank {
            getApplication<Application>().getString(R.string.profile_default_name)
        }
        _riderId.value = getApplication<Application>().getString(
            R.string.profile_rider_id,
            getApplication<Application>().getString(R.string.profile_rider_id_stub)
        )
        _phone.value = ProfileDisplayFormatter.formatPhone(KycProgressRepository.sessionMobile())
        _email.value = ProfileDisplayFormatter.formatEmail(personal.email)
        _location.value = ProfileDisplayFormatter.formatLocation(address.city, address.state)
        _menuItems.value = ProfileRepository.menuItems(kycApproved = status == KycStatus.APPROVED)
    }

    fun onEdit() {
        _toastMessage.value = R.string.profile_edit_stub
    }

    fun onMenuClicked(action: ProfileMenuAction) {
        when (action) {
            ProfileMenuAction.SETTINGS, ProfileMenuAction.NOTIFICATIONS -> {
                _navEvent.value = ProfileNavEvent.OpenSettings
            }
            ProfileMenuAction.DOCUMENTS -> {
                _navEvent.value = ProfileNavEvent.OpenDocuments
            }
            ProfileMenuAction.HELP -> {
                _navEvent.value = ProfileNavEvent.ShowStub(R.string.profile_menu_help)
            }
            ProfileMenuAction.PRIVACY -> {
                _navEvent.value = ProfileNavEvent.ShowStub(R.string.profile_menu_privacy)
            }
        }
    }

    fun onLogout() {
        _navEvent.value = ProfileNavEvent.Logout
    }

    fun clearNavEvent() {
        _navEvent.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
