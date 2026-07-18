package com.example.transcilmobileapp.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.transcilmobileapp.core.Gender

class CreatePersonalAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedGender = MutableLiveData<Gender?>(null)
    val selectedGender: LiveData<Gender?> = _selectedGender

    private val _dateOfBirth = MutableLiveData<String>()
    val dateOfBirth: LiveData<String> = _dateOfBirth

    private val _navigateNext = MutableLiveData<Boolean>()
    val navigateNext: LiveData<Boolean> = _navigateNext

    private val _fieldErrors = MutableLiveData(PersonalFieldErrors())
    val fieldErrors: LiveData<PersonalFieldErrors> = _fieldErrors

    fun onGenderSelected(gender: Gender) {
        _selectedGender.value = gender
        clearGenderError()
    }

    fun onDateOfBirthSelected(formatted: String) {
        _dateOfBirth.value = formatted
        clearDobError()
    }

    fun clearFullNameError() {
        val current = _fieldErrors.value ?: PersonalFieldErrors()
        if (current.fullName != null) {
            _fieldErrors.value = current.copy(fullName = null)
        }
    }

    fun clearEmailError() {
        val current = _fieldErrors.value ?: PersonalFieldErrors()
        if (current.email != null) {
            _fieldErrors.value = current.copy(email = null)
        }
    }

    fun clearDobError() {
        val current = _fieldErrors.value ?: PersonalFieldErrors()
        if (current.dateOfBirth != null) {
            _fieldErrors.value = current.copy(dateOfBirth = null)
        }
    }

    fun clearGenderError() {
        val current = _fieldErrors.value ?: PersonalFieldErrors()
        if (current.gender != null) {
            _fieldErrors.value = current.copy(gender = null)
        }
    }

    fun hydrateFromDraft() {
        val draft = KycProgressRepository.personalDraft()
        if (draft.dateOfBirth.isNotBlank()) {
            _dateOfBirth.value = draft.dateOfBirth
        }
        if (draft.gender != null) {
            _selectedGender.value = draft.gender
        }
    }

    fun onContinueClicked(fullName: String, email: String) {
        val errors = PersonalDetailsValidator.validate(
            fullName = fullName,
            email = email,
            dateOfBirth = _dateOfBirth.value.orEmpty(),
            gender = _selectedGender.value
        )
        _fieldErrors.value = errors
        if (errors.hasErrors) return

        KycProgressRepository.savePersonal(
            PersonalDraft(
                fullName = fullName.trim(),
                email = email.trim(),
                dateOfBirth = _dateOfBirth.value.orEmpty(),
                gender = _selectedGender.value
            )
        )
        _navigateNext.value = true
    }
}
