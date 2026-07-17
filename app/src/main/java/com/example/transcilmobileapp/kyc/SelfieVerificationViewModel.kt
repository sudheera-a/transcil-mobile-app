package com.example.transcilmobileapp.kyc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.transcilmobileapp.core.BaseViewModel

enum class SelfieUiState {
    CAPTURE,
    REVIEW
}

class SelfieVerificationViewModel : BaseViewModel() {

    private val _uiState = MutableLiveData(SelfieUiState.CAPTURE)
    val uiState: LiveData<SelfieUiState> = _uiState

    private val _navigateToPending = MutableLiveData<Boolean>()
    val navigateToPending: LiveData<Boolean> = _navigateToPending

    fun onCapture() {
        _uiState.value = SelfieUiState.REVIEW
    }

    fun onRetake() {
        _uiState.value = SelfieUiState.CAPTURE
    }

    fun onContinue() {
        if (_uiState.value != SelfieUiState.REVIEW) return
        KycProgressRepository.markCompleted(KycStep.SELFIE)
        _navigateToPending.value = true
    }
}
