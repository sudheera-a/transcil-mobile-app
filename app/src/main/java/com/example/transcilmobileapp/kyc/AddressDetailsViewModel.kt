package com.example.transcilmobileapp.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.transcilmobileapp.R

class AddressDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val _navigateNext = MutableLiveData<Boolean>()
    val navigateNext: LiveData<Boolean> = _navigateNext

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun onContinueClicked(
        line1: String,
        line2: String,
        city: String,
        state: String,
        pincode: String
    ) {
        if (line1.isBlank() || city.isBlank() || state.isBlank() || pincode.isBlank()) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_required_fields)
            return
        }
        val digits = pincode.filter { it.isDigit() }
        if (digits.length != 6) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_invalid_pincode)
            return
        }
        KycProgressRepository.saveAddress(
            AddressDraft(
                line1 = line1.trim(),
                line2 = line2.trim(),
                city = city.trim(),
                state = state.trim(),
                pincode = digits
            )
        )
        _navigateNext.value = true
    }
}
