package com.example.transcilmobileapp.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseViewModel

data class OnboardingPage(
    val imageRes: Int,
    val titleRes: Int,
    val descRes: Int,
)

class OnboardingViewModel : BaseViewModel() {

    val pages = listOf(
        OnboardingPage(R.drawable.scooter_onboarding, R.string.onboarding1_title, R.string.onboarding1_desc),
        OnboardingPage(R.drawable.network_onboarding, R.string.onboarding2_title, R.string.onboarding2_desc),
        OnboardingPage(R.drawable.img_ev_scooter, R.string.onboarding3_title, R.string.onboarding3_desc),
    )

    private val _pageIndex = MutableLiveData(0)
    val pageIndex: LiveData<Int> = _pageIndex

    private val _navigateToWelcome = MutableLiveData(false)
    val navigateToWelcome: LiveData<Boolean> = _navigateToWelcome

    fun onNextClicked() {
        val next = (_pageIndex.value ?: 0) + 1
        if (next >= pages.size) {
            _navigateToWelcome.value = true
        } else {
            _pageIndex.value = next
        }
    }

    fun onSkipClicked() {
        _navigateToWelcome.value = true
    }
}
