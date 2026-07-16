package com.example.transcilmobileapp

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class CreatePersonalAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedGender = MutableLiveData(Gender.MALE)
    val selectedGender: LiveData<Gender> = _selectedGender

    private val _dateOfBirth = MutableLiveData<String>()
    val dateOfBirth: LiveData<String> = _dateOfBirth

    private val _navigateNext = MutableLiveData<Boolean>()
    val navigateNext: LiveData<Boolean> = _navigateNext

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun onGenderSelected(gender: Gender) {
        _selectedGender.value = gender
    }

    fun onDateOfBirthSelected(formatted: String) {
        _dateOfBirth.value = formatted
    }

    fun onContinueClicked(fullName: String, email: String) {
        val dob = _dateOfBirth.value.orEmpty()
        if (fullName.isBlank() || email.isBlank() || dob.isBlank()) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_required_fields)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_invalid_email)
            return
        }
        _navigateNext.value = true
    }
}
