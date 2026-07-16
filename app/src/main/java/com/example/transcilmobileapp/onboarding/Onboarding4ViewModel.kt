package com.example.transcilmobileapp.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.transcilmobileapp.core.BaseViewModel

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