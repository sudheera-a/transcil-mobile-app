package com.example.transcilmobileapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class Onboarding3ViewModel : BaseViewModel() {

    private val _navigateToOnboarding4 = MutableLiveData<Boolean>()
    val navigateToOnboarding4: LiveData<Boolean> get() = _navigateToOnboarding4

    private val _skipOnboarding = MutableLiveData<Boolean>()
    val skipOnboarding: LiveData<Boolean> get() = _skipOnboarding

    fun onNextClicked() {
        _navigateToOnboarding4.value = true
    }

    fun onSkipClicked() {
        _skipOnboarding.value = true
    }
}