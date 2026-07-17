package com.example.transcilmobileapp.journey

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.transcilmobileapp.core.BaseViewModel
import com.example.transcilmobileapp.core.JourneyType

class ChooseJourneyViewModel : BaseViewModel() {

    private val _selectedJourney = MutableLiveData<JourneyType?>()
    val selectedJourney: LiveData<JourneyType?> = _selectedJourney

    private val _continueEnabled = MutableLiveData(false)
    val continueEnabled: LiveData<Boolean> = _continueEnabled

    private val _navigateToPersonalAccount = MutableLiveData<Boolean>()
    val navigateToPersonalAccount: LiveData<Boolean> = _navigateToPersonalAccount

    fun onJourneySelected(type: JourneyType) {
        _selectedJourney.value = type
        _continueEnabled.value = true
    }

    fun onContinueClicked() {
        when (_selectedJourney.value) {
            JourneyType.RENT_EV, JourneyType.THREE_PL ->
                _navigateToPersonalAccount.value = true
            null -> Unit
        }
    }
}
