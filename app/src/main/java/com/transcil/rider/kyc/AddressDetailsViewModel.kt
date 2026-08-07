/**
 * ViewModel for AddressDetailsActivity — loads states/cities from API, validates, saves address.
 * Prefills from local draft or GET /me/address; marks ADDRESS step complete on successful PUT.
 */
package com.transcil.rider.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.transcil.rider.data.model.onboarding.StateOption
import com.transcil.rider.repository.OnboardingRepository
import kotlinx.coroutines.launch

/** `data class`: address fields to pre-populate the form when returning to this screen. */
data class AddressPrefill(
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
)

class AddressDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val onboardingRepository = OnboardingRepository()

    private val _navigateNext = MutableLiveData<Boolean>()
    val navigateNext: LiveData<Boolean> = _navigateNext

    private val _fieldErrors = MutableLiveData(AddressFieldErrors())
    val fieldErrors: LiveData<AddressFieldErrors> = _fieldErrors

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _states = MutableLiveData<List<StateOption>>(emptyList())
    val states: LiveData<List<StateOption>> = _states

    private val _cities = MutableLiveData<List<String>>(emptyList())
    val cities: LiveData<List<String>> = _cities

    private val _prefill = MutableLiveData<AddressPrefill>()
    val prefill: LiveData<AddressPrefill> = _prefill

    fun clearLine1Error() = clear { it.copy(line1 = null) }
    fun clearLine2Error() = clear { it.copy(line2 = null) }
    fun clearCityError() = clear { it.copy(city = null) }
    fun clearStateError() = clear { it.copy(state = null) }
    fun clearPincodeError() = clear { it.copy(pincode = null) }

    private fun clear(transform: (AddressFieldErrors) -> AddressFieldErrors) {
        val current = _fieldErrors.value ?: AddressFieldErrors()
        _fieldErrors.value = transform(current)
    }

    /** `suspend fun` calls inside viewModelScope.launch — fetches states, cities, and saved address. */
    fun load() {
        val draft = KycProgressRepository.addressDraft()
        if (draft.line1.isNotBlank() || draft.city.isNotBlank() || draft.state.isNotBlank()) {
            _prefill.value = AddressPrefill(
                line1 = draft.line1,
                line2 = draft.line2,
                city = draft.city,
                state = draft.state,
                pincode = draft.pincode,
            )
        }
        viewModelScope.launch {
            val apiStates = onboardingRepository.listStates().getOrNull()
            _states.value = if (!apiStates.isNullOrEmpty()) {
                apiStates
            } else {
                IndianStates.ALL.map { StateOption(code = it, name = it) }
            }
            onboardingRepository.getAddress().onSuccess { address ->
                _prefill.value = AddressPrefill(
                    line1 = address.addressLine1.orEmpty(),
                    line2 = address.addressLine2.orEmpty(),
                    city = address.city.orEmpty(),
                    state = address.state.orEmpty(),
                    pincode = address.pincode.orEmpty(),
                )
            }
        }
    }

    fun onStateSelected(option: StateOption?) {
        clearStateError()
        val code = option?.code.orEmpty()
        if (code.isBlank() || option?.name == IndianStates.PLACEHOLDER) {
            _cities.value = emptyList()
            return
        }
        viewModelScope.launch {
            _cities.value = onboardingRepository.listCities(code).getOrDefault(emptyList())
        }
    }

    fun onContinueClicked(
        line1: String,
        line2: String,
        city: String,
        stateOption: StateOption?,
        pincode: String,
    ) {
        val stateValue = stateOption?.code?.takeIf { it.isNotBlank() && stateOption.name != IndianStates.PLACEHOLDER }
            .orEmpty()
        val allowed = _states.value
            ?.map { it.code }
            ?.filter { it.isNotBlank() }
            ?.toSet()
        val errors = AddressDetailsValidator.validate(
            line1 = line1,
            line2 = line2,
            city = city,
            state = stateValue,
            pincode = pincode,
            allowedStates = allowed,
        )
        _fieldErrors.value = errors
        if (errors.hasErrors) return

        viewModelScope.launch {
            _isLoading.value = true
            onboardingRepository.putAddress(line1, line2, city, stateValue, pincode)
                .onSuccess {
                    KycProgressRepository.saveAddress(
                        AddressDraft(
                            line1 = line1.trim(),
                            line2 = line2.trim(),
                            city = city.trim(),
                            state = stateValue,
                            pincode = pincode.filter { it.isDigit() }.take(6),
                        )
                    )
                    KycProgressRepository.markCompleted(KycStep.ADDRESS)
                    _navigateNext.value = true
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to save address"
                }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
