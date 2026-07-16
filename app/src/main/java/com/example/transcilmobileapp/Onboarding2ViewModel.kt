package com.example.transcilmobileapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class Onboarding2ViewModel : BaseViewModel() {

    private val _navigateToOnboarding3 = MutableLiveData<Boolean>()
    val navigateToOnboarding3: LiveData<Boolean> get() = _navigateToOnboarding3

    private val _skipOnboarding = MutableLiveData<Boolean>()
    val skipOnboarding: LiveData<Boolean> get() = _skipOnboarding

    fun onNextClicked() {
        _navigateToOnboarding3.value = true
    }

    fun onSkipClicked() {
        _skipOnboarding.value = true
    }
}