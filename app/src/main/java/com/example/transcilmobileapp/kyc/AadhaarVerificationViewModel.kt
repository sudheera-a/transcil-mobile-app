package com.example.transcilmobileapp.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.transcilmobileapp.R

class AadhaarVerificationViewModel(application: Application) : AndroidViewModel(application) {

    private val _consentChecked = MutableLiveData(false)
    val consentChecked: LiveData<Boolean> = _consentChecked

    private val _navigateToOtp = MutableLiveData<Boolean>()
    val navigateToOtp: LiveData<Boolean> = _navigateToOtp

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
        _navigateToOtp.value = true
    }

    fun onSkipClicked() {
        _skipFlow.value = true
    }
}
