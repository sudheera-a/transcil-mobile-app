/**
 * ViewModel for PAN verification — validates format, calls verifyPan API, syncs onboarding on success.
 * Supports skip (client-only navigation; server step stays pending) and name-mismatch warnings.
 */
package com.transcil.rider.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.transcil.rider.R
import com.transcil.rider.repository.OnboardingRepository
import kotlinx.coroutines.launch

class PanVerificationViewModel(application: Application) : AndroidViewModel(application) {

    private val onboardingRepository = OnboardingRepository()

    private val _navigateNext = MutableLiveData<Boolean>()
    val navigateNext: LiveData<Boolean> = _navigateNext

    private val _skipToHome = MutableLiveData<Boolean>()
    val skipToHome: LiveData<Boolean> = _skipToHome

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _warningMessage = MutableLiveData<String?>()
    val warningMessage: LiveData<String?> = _warningMessage

    fun onVerifyClicked(pan: String) {
        val normalized = pan.trim().uppercase()
        if (!PAN_REGEX.matches(normalized)) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_invalid_pan)
            return
        }
        val name = KycProgressRepository.personalDraft().fullName.trim()
        if (name.isBlank()) {
            _errorMessage.value =
                getApplication<Application>().getString(R.string.kyc_digio_need_personal_name)
            return
        }
        val dob = KycProgressRepository.personalDraft().dateOfBirth.takeIf { it.isNotBlank() }
        // `suspend fun` via repository — must run inside viewModelScope.launch (coroutine).
        viewModelScope.launch {
            onboardingRepository.verifyPan(normalized, name, dob)
                .onSuccess { data ->
                    if (!data.nameMatch) {
                        val registered = data.registeredName?.takeIf { it.isNotBlank() }
                        _warningMessage.value = if (registered != null) {
                            getApplication<Application>().getString(
                                R.string.kyc_pan_name_mismatch,
                                registered,
                            )
                        } else {
                            getApplication<Application>().getString(R.string.kyc_pan_name_mismatch_generic)
                        }
                    }
                    onboardingRepository.getOnboarding()
                        .onSuccess { OnboardingSync.apply(it) }
                    _navigateNext.value = true
                }
                .onFailure { e ->
                    _errorMessage.value = e.message
                        ?: getApplication<Application>().getString(R.string.error_invalid_pan)
                }
        }
    }

    fun onSkipClicked() {
        // Client navigation only — PAN step stays pending on server.
        _skipToHome.value = true
    }

    fun clearWarning() {
        _warningMessage.value = null
    }

    /** `companion object`: static-like members on the class — shared PAN regex, no instance needed. */
    companion object {
        private val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")
    }
}
