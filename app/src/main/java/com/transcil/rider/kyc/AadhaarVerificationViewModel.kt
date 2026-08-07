/**
 * ViewModel for AadhaarVerificationActivity — validates input, starts Digio e-KYC session.
 * Exposes gateway URL via LiveData for the Activity to open in a Custom Tab.
 */
package com.transcil.rider.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.transcil.rider.R
import com.transcil.rider.repository.DigioKycRepository
import kotlinx.coroutines.launch

class AadhaarVerificationViewModel(application: Application) : AndroidViewModel(application) {

    private val _consentChecked = MutableLiveData(false)
    val consentChecked: LiveData<Boolean> = _consentChecked

    private val _openDigioUrl = MutableLiveData<String?>()
    val openDigioUrl: LiveData<String?> = _openDigioUrl

    private val _skipFlow = MutableLiveData<Boolean>()
    val skipFlow: LiveData<Boolean> = _skipFlow

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun onConsentChanged(checked: Boolean) {
        _consentChecked.value = checked
    }

    fun onVerifyClicked(aadhaar: String) {
        val digits = aadhaar.filter { it.isDigit() }
        if (digits.length != 12) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_invalid_aadhaar)
            return
        }
        if (_consentChecked.value != true) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_aadhaar_consent)
            return
        }
        val name = KycProgressRepository.personalDraft().fullName.trim()
        if (name.isBlank() || name.any { it.isDigit() }) {
            _errorMessage.value =
                getApplication<Application>().getString(R.string.kyc_digio_need_personal_name)
            return
        }
        viewModelScope.launch {
            // DigioKycRepository.start is suspend — network call to obtain gateway URL.
            DigioKycRepository().start(name)
                .onSuccess { _openDigioUrl.value = it.gatewayUrl }
                .onFailure {
                    _errorMessage.value = it.message?.takeIf { msg -> msg.isNotBlank() }
                        ?: getApplication<Application>().getString(R.string.kyc_digio_failed)
                }
        }
    }

    fun onSkipClicked() {
        _skipFlow.value = true
    }

    fun clearOpenDigioUrl() {
        _openDigioUrl.value = null
    }
}
