/**
 * ViewModel for ChooseJourneyActivity — loads enabled roles from API, saves rider role on continue.
 * Starting a journey initializes KycProgressRepository and navigates to personal-details form.
 */
package com.transcil.rider.journey

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.transcil.rider.core.BaseViewModel
import com.transcil.rider.core.JourneyType
import com.transcil.rider.kyc.KycProgressRepository
import com.transcil.rider.repository.OnboardingRepository
import com.transcil.rider.repository.toRiderRole
import kotlinx.coroutines.launch

class ChooseJourneyViewModel(
    private val onboardingRepository: OnboardingRepository = OnboardingRepository(),
) : BaseViewModel() {

    private val _selectedJourney = MutableLiveData<JourneyType?>()
    val selectedJourney: LiveData<JourneyType?> = _selectedJourney

    private val _continueEnabled = MutableLiveData(false)
    val continueEnabled: LiveData<Boolean> = _continueEnabled

    private val _navigateToPersonalAccount = MutableLiveData<Boolean>()
    val navigateToPersonalAccount: LiveData<Boolean> = _navigateToPersonalAccount

    private val _enabledRoles = MutableLiveData<Set<String>>(emptySet())
    val enabledRoles: LiveData<Set<String>> = _enabledRoles

    init {
        loadJourneyOptions()
    }

    /** `suspend fun` via repository — fetches which journey cards the server allows. */
    fun loadJourneyOptions() {
        viewModelScope.launch {
            onboardingRepository.journeyOptions()
                .onSuccess { options ->
                    _enabledRoles.value = options.map { it.roleKey.lowercase() }.toSet()
                }
                .onFailure {
                    // Soft-fail: hard-coded cards stay selectable.
                    _enabledRoles.value = setOf("rider", "3pl")
                }
        }
    }

    fun onJourneySelected(type: JourneyType) {
        val roles = _enabledRoles.value.orEmpty()
        val role = type.toRiderRole()
        if (roles.isNotEmpty() && role !in roles) {
            showError("This journey is not available right now")
            return
        }
        _selectedJourney.value = type
        _continueEnabled.value = true
    }

    fun onContinueClicked() {
        val type = _selectedJourney.value ?: return
        viewModelScope.launch {
            showLoading()
            // setRiderRole is suspend — persists journey choice before KYC begins.
            onboardingRepository.setRiderRole(type)
                .onSuccess {
                    KycProgressRepository.startJourney(type)
                    _navigateToPersonalAccount.value = true
                }
                .onFailure { e ->
                    showError(e.message ?: "Failed to save journey")
                }
            hideLoading()
        }
    }
}
