package com.example.transcilmobileapp.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.transcilmobileapp.core.BaseViewModel
import com.example.transcilmobileapp.data.local.TokenStore
import com.example.transcilmobileapp.repository.AuthRepository
import kotlinx.coroutines.launch

class VerifyOtpViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
) : BaseViewModel() {

    private val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean> get() = _navigateToHome

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
                    _navigateToHome.value = true
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
