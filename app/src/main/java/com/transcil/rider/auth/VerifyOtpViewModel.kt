/**
 * ViewModel for [VerifyOtpActivity]: verifies OTP with the server, saves tokens, triggers post-login navigation.
 * On success writes access/refresh tokens via [TokenStore] and signals navigation to journey selection.
 *
 * Kotlin notes:
 * - `suspend fun` lives in [AuthRepository]; ViewModel calls it inside [viewModelScope.launch].
 * - [Result] wraps API success/failure so callers use onSuccess/onFailure instead of exceptions in UI code.
 */
package com.transcil.rider.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.transcil.rider.core.BaseViewModel
import com.transcil.rider.data.local.TokenStore
import com.transcil.rider.repository.AuthRepository
import kotlinx.coroutines.launch

class VerifyOtpViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
) : BaseViewModel() {

    private val _navigateToChooseJourney = MutableLiveData<Boolean>()
    val navigateToChooseJourney: LiveData<Boolean> get() = _navigateToChooseJourney

    // Resend OTP returns a new session id; Activity observes and updates local otpSession.
    private val _otpSession = MutableLiveData<String>()
    val otpSession: LiveData<String> get() = _otpSession

    fun onVerifyClicked(session: String, mobile: String, otp: String) {
        if (otp.length != 6) {
            showError("Please enter the complete 6-digit OTP")
            return
        }
        viewModelScope.launch {
            showLoading()
            authRepository.verify(session, mobile, otp)
                .onSuccess { tokens ->
                    TokenStore.save(tokens.accessToken, tokens.refreshToken)
                    _navigateToChooseJourney.value = true
                }
                .onFailure { e ->
                    showError(e.message ?: "OTP verification failed")
                }
            hideLoading()
        }
    }

    fun onResendClicked(mobile: String) {
        viewModelScope.launch {
            showLoading()
            authRepository.start(mobile)
                .onSuccess { data ->
                    _otpSession.value = data.session
                }
                .onFailure { e ->
                    showError(e.message ?: "Failed to resend OTP")
                }
            hideLoading()
        }
    }
}
