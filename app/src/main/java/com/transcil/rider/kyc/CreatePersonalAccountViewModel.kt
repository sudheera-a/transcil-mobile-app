/**
 * ViewModel for CreatePersonalAccountActivity — loads profile prefill, validates, PATCHes profile.
 * First server-backed KYC step; marks PERSONAL complete and navigates to progress hub on success.
 */
package com.transcil.rider.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.transcil.rider.core.Gender
import com.transcil.rider.repository.OnboardingRepository
import com.transcil.rider.repository.fromApiDob
import com.transcil.rider.repository.parseApiGender
import kotlinx.coroutines.launch

data class PersonalPrefill(
    val fullName: String = "",
    val email: String = "",
)

class CreatePersonalAccountViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val onboardingRepository = OnboardingRepository()

    private val _selectedGender = MutableLiveData<Gender?>(null)
    val selectedGender: LiveData<Gender?> = _selectedGender

    private val _dateOfBirth = MutableLiveData<String>()
    val dateOfBirth: LiveData<String> = _dateOfBirth

    private val _prefill = MutableLiveData<PersonalPrefill>()
    val prefill: LiveData<PersonalPrefill> = _prefill

    private val _navigateNext = MutableLiveData<Boolean>()
    val navigateNext: LiveData<Boolean> = _navigateNext

    private val _fieldErrors = MutableLiveData(PersonalFieldErrors())
    val fieldErrors: LiveData<PersonalFieldErrors> = _fieldErrors

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

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

    fun load() {
        val draft = KycProgressRepository.personalDraft()
        if (draft.dateOfBirth.isNotBlank()) _dateOfBirth.value = draft.dateOfBirth
        if (draft.gender != null) _selectedGender.value = draft.gender
        if (draft.fullName.isNotBlank() || draft.email.isNotBlank()) {
            _prefill.value = PersonalPrefill(draft.fullName, draft.email)
        }
        viewModelScope.launch {
            onboardingRepository.getProfile().onSuccess { profile ->
                val name = profile.displayName?.takeIf { it.isNotBlank() }
                    ?: listOfNotNull(profile.givenName, profile.familyName)
                        .joinToString(" ")
                        .trim()
                val email = profile.email.orEmpty()
                if (name.isNotBlank() || email.isNotBlank()) {
                    _prefill.value = PersonalPrefill(name, email)
                }
                profile.dob?.takeIf { it.isNotBlank() }?.let { _dateOfBirth.value = fromApiDob(it) }
                parseApiGender(profile.gender)?.let { _selectedGender.value = it }
            }
        }
    }

    fun onContinueClicked(fullName: String, email: String) {
        val gender = _selectedGender.value
        val dob = _dateOfBirth.value.orEmpty()
        val errors = PersonalDetailsValidator.validate(
            fullName = fullName,
            email = email,
            dateOfBirth = dob,
            gender = gender,
        )
        _fieldErrors.value = errors
        if (errors.hasErrors || gender == null) return

        viewModelScope.launch {
            // PATCH profile is suspend — network I/O off the main thread.
            _isLoading.value = true
            onboardingRepository.patchProfile(fullName, email, dob, gender)
                .onSuccess {
                    KycProgressRepository.savePersonal(
                        PersonalDraft(
                            fullName = fullName.trim(),
                            email = email.trim(),
                            dateOfBirth = dob,
                            gender = gender,
                        )
                    )
                    KycProgressRepository.markCompleted(KycStep.PERSONAL)
                    _navigateNext.value = true
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to save profile"
                }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
