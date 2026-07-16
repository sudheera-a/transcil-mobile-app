package com.example.transcilmobileapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class BankDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val _navigateNext = MutableLiveData<Boolean>()
    val navigateNext: LiveData<Boolean> = _navigateNext

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun onVerifyClicked(
        holderName: String,
        accountNumber: String,
        confirmAccountNumber: String,
        ifsc: String
    ) {
        if (holderName.isBlank() || accountNumber.isBlank() || confirmAccountNumber.isBlank() || ifsc.isBlank()) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_required_fields)
            return
        }
        if (accountNumber != confirmAccountNumber) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_account_mismatch)
            return
        }
        val normalizedIfsc = ifsc.trim().uppercase()
        if (!IFSC_REGEX.matches(normalizedIfsc)) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_invalid_ifsc)
            return
        }
        _navigateNext.value = true
    }

    fun onSkipClicked() {
        _navigateNext.value = true
    }

    companion object {
        private val IFSC_REGEX = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
    }
}
