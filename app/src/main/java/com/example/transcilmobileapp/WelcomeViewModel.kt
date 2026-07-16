package com.example.transcilmobileapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class WelcomeViewModel : BaseViewModel() {

    private val _navigateToVerifyOtp = MutableLiveData<String>()
    val navigateToVerifyOtp: LiveData<String> get() = _navigateToVerifyOtp

    fun onSendOtpClicked(mobileNumber: String) {
        if (mobileNumber.length != 10) {
            showError("Please enter a valid 10-digit mobile number")
            return
        }
        _navigateToVerifyOtp.value = mobileNumber
    }
}