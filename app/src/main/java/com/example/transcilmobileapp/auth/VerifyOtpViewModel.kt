package com.example.transcilmobileapp.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.transcilmobileapp.core.BaseViewModel

class VerifyOtpViewModel : BaseViewModel() {

    private val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean> get() = _navigateToHome

    fun onVerifyClicked(otp: String) {
        if (otp.length != 6) {
            showError("Please enter the complete 6-digit OTP")
            return
        }
        // Later: call an actual verification API here
        _navigateToHome.value = true
    }
}