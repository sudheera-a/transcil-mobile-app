package com.example.transcilmobileapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ChooseJourneyViewModel : BaseViewModel() {

    private val _selectedJourney = MutableLiveData<JourneyType?>()
    val selectedJourney: LiveData<JourneyType?> = _selectedJourney

    private val _continueEnabled = MutableLiveData(false)
    val continueEnabled: LiveData<Boolean> = _continueEnabled

    private val _navigateToPersonalAccount = MutableLiveData<Boolean>()
    val navigateToPersonalAccount: LiveData<Boolean> = _navigateToPersonalAccount

    private val _showComingSoon = MutableLiveData<Boolean>()
    val showComingSoon: LiveData<Boolean> = _showComingSoon

    fun onJourneySelected(type: JourneyType) {
        _selectedJourney.value = type
        _continueEnabled.value = true
    }

    fun onContinueClicked() {
        when (_selectedJourney.value) {
            JourneyType.RENT_EV -> _navigateToPersonalAccount.value = true
            JourneyType.THREE_PL -> _showComingSoon.value = true
            null -> Unit
        }
    }
}
