package com.example.transcilmobileapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class Onboarding4ViewModel : BaseViewModel() {

    private val _navigateToWelcome = MutableLiveData<Boolean>()
    val navigateToWelcome: LiveData<Boolean> get() = _navigateToWelcome

    fun onGetStartedClicked() {
        _navigateToWelcome.value = true
    }

    fun onSkipClicked() {
        _navigateToWelcome.value = true
    }
}