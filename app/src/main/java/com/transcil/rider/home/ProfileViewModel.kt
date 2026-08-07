/**
 * Profile screen logic: hydrates fields from KYC drafts and emits one-shot nav/toast events.
 * [ProfileNavEvent] is a sealed hierarchy so the Fragment handles every outcome exhaustively.
 */
package com.transcil.rider.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.transcil.rider.R
import com.transcil.rider.auth.AuthSession
import com.transcil.rider.core.KycStatus
import com.transcil.rider.kyc.KycProgressRepository
import kotlinx.coroutines.launch

// sealed class = restricted subtype set; compiler checks when branches are complete.
sealed class ProfileNavEvent {
    data object OpenSettings : ProfileNavEvent()
    data object OpenDocuments : ProfileNavEvent()
    data class OpenContent(val page: ContentPage) : ProfileNavEvent()
    data class ShowStub(val titleRes: Int) : ProfileNavEvent()
    data object SignedOut : ProfileNavEvent()
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

    @Volatile private var signingOut = false

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
                _navEvent.value = ProfileNavEvent.OpenContent(ContentPage.HELP)
            }
            ProfileMenuAction.PRIVACY -> {
                _navEvent.value = ProfileNavEvent.OpenContent(ContentPage.PRIVACY)
            }
        }
    }

    fun onLogout() {
        if (signingOut) return
        signingOut = true
        // viewModelScope.launch runs suspend work off the main thread inside the ViewModel.
        viewModelScope.launch {
            AuthSession.signOut()
            _navEvent.value = ProfileNavEvent.SignedOut
        }
    }

    fun clearNavEvent() {
        _navEvent.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
