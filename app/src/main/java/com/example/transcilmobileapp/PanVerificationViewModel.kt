package com.example.transcilmobileapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class PanVerificationViewModel(application: Application) : AndroidViewModel(application) {

    private val _navigateNext = MutableLiveData<Boolean>()
    val navigateNext: LiveData<Boolean> = _navigateNext

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun onVerifyClicked(pan: String) {
        val normalized = pan.trim().uppercase()
        if (!PAN_REGEX.matches(normalized)) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_invalid_pan)
            return
        }
        _navigateNext.value = true
    }

    fun onSkipClicked() {
        _navigateNext.value = true
    }

    companion object {
        private val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")
    }
}
