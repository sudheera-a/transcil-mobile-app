package com.example.transcilmobileapp.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AddressDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val _navigateNext = MutableLiveData<Boolean>()
    val navigateNext: LiveData<Boolean> = _navigateNext

    private val _fieldErrors = MutableLiveData(AddressFieldErrors())
    val fieldErrors: LiveData<AddressFieldErrors> = _fieldErrors

    fun clearLine1Error() = clear { it.copy(line1 = null) }
    fun clearLine2Error() = clear { it.copy(line2 = null) }
    fun clearCityError() = clear { it.copy(city = null) }
    fun clearStateError() = clear { it.copy(state = null) }
    fun clearPincodeError() = clear { it.copy(pincode = null) }

    private fun clear(transform: (AddressFieldErrors) -> AddressFieldErrors) {
        val current = _fieldErrors.value ?: AddressFieldErrors()
        _fieldErrors.value = transform(current)
    }

    fun onContinueClicked(
        line1: String,
        line2: String,
        city: String,
        state: String,
        pincode: String
    ) {
        val errors = AddressDetailsValidator.validate(line1, line2, city, state, pincode)
        _fieldErrors.value = errors
        if (errors.hasErrors) return

        KycProgressRepository.saveAddress(
            AddressDraft(
                line1 = line1.trim(),
                line2 = line2.trim(),
                city = city.trim(),
                state = state.trim(),
                pincode = pincode.filter { it.isDigit() }.take(6)
            )
        )
        _navigateNext.value = true
    }
}
