package com.example.transcilmobileapp.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.transcilmobileapp.core.BaseViewModel

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