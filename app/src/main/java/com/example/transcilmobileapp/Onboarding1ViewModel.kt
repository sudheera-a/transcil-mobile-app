package com.example.transcilmobileapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class Onboarding1ViewModel : BaseViewModel() {

    private val _navigateToOnboarding2 = MutableLiveData<Boolean>()
    val navigateToOnboarding2: LiveData<Boolean> get() = _navigateToOnboarding2

    fun onNextClicked() {
        _navigateToOnboarding2.value = true
    }
}