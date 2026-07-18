package com.example.transcilmobileapp.kyc

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
        val errors = BankDetailsValidator.validate(
            holderName = holderName,
            accountNumber = accountNumber,
            confirmAccountNumber = confirmAccountNumber,
            ifsc = ifsc,
            consent = true
        )
        if (errors.hasErrors) {
            val first = listOfNotNull(
                errors.holderName,
                errors.accountNumber,
                errors.confirmAccountNumber,
                errors.ifsc
            ).firstOrNull()
            if (first != null) {
                _errorMessage.value = getApplication<Application>().getString(first)
            }
            return
        }
        KycProgressRepository.saveBank(
            BankDraft(
                holderName = holderName.trim(),
                accountNumber = accountNumber.filter { it.isDigit() }.take(18),
                confirmAccountNumber = confirmAccountNumber.filter { it.isDigit() }.take(18),
                ifsc = ifsc.trim().uppercase().take(11),
                consent = true
            )
        )
        _navigateNext.value = true
    }

    private val _skipToHome = MutableLiveData<Boolean>()
    val skipToHome: LiveData<Boolean> = _skipToHome

    fun onSkipClicked() {
        _skipToHome.value = true
    }
}
