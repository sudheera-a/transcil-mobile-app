/**
 * ViewModel for [WelcomeActivity]: validates mobile input and calls the auth API to start OTP.
 * Sits between the UI and [AuthRepository]; does not know about Activities or Intents.
 *
 * Kotlin notes:
 * - `data class` = auto-generates equals/hashCode/toString for a simple value holder ([NavigateToOtp]).
 * - [viewModelScope.launch] = run coroutine tied to ViewModel lifetime (cancelled if screen destroyed).
 * - [Result.onSuccess/onFailure] = idiomatic Kotlin for success vs error without try/catch at call site.
 */
package com.transcil.rider.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.transcil.rider.core.BaseViewModel
import com.transcil.rider.repository.AuthRepository
import kotlinx.coroutines.launch

/** Payload for navigating Welcome → Verify OTP (mobile + server session id). */
data class NavigateToOtp(val mobile: String, val session: String)

class WelcomeViewModel(
    // Default param lets production code use real API; tests can inject a fake repository.
    private val authRepository: AuthRepository = AuthRepository(),
) : BaseViewModel() {

    private val _navigateToVerifyOtp = MutableLiveData<NavigateToOtp>()
    val navigateToVerifyOtp: LiveData<NavigateToOtp> get() = _navigateToVerifyOtp

    fun onSendOtpClicked(mobileNumber: String) {
        if (mobileNumber.length != 10) {
            showError("Please enter a valid 10-digit mobile number")
            return
        }
        viewModelScope.launch {
            showLoading()
            authRepository.start(mobileNumber)
                .onSuccess { data ->
                    _navigateToVerifyOtp.value = NavigateToOtp(mobileNumber, data.session)
                }
                .onFailure { e ->
                    showError(e.message ?: "Failed to send OTP")
                }
            hideLoading()
        }
    }
}
