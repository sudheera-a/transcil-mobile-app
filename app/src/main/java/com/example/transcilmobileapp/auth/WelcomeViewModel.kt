package com.example.transcilmobileapp.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.transcilmobileapp.core.BaseViewModel
import com.example.transcilmobileapp.repository.AuthRepository
import kotlinx.coroutines.launch

data class NavigateToOtp(val mobile: String, val session: String)

class WelcomeViewModel(
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
