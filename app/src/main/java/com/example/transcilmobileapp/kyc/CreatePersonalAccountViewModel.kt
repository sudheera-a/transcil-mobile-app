package com.example.transcilmobileapp.kyc

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.Gender

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

    fun hydrateFromDraft() {
        val draft = KycProgressRepository.personalDraft()
        if (draft.fullName.isNotBlank() || draft.email.isNotBlank() || draft.dateOfBirth.isNotBlank()) {
            if (draft.dateOfBirth.isNotBlank()) {
                _dateOfBirth.value = draft.dateOfBirth
            }
            if (draft.gender != null) {
                _selectedGender.value = draft.gender
            }
        }
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
        KycProgressRepository.savePersonal(
            PersonalDraft(
                fullName = fullName.trim(),
                email = email.trim(),
                dateOfBirth = dob,
                gender = _selectedGender.value
            )
        )
        _navigateNext.value = true
    }
}
