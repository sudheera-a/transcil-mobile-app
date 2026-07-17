package com.example.transcilmobileapp.kyc

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivitySelfieVerificationBinding

import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.KycNavigator

class SelfieVerificationActivity :
    BaseActivity<ActivitySelfieVerificationBinding>(ActivitySelfieVerificationBinding::inflate) {

    private val viewModel: SelfieVerificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.ivBack.setOnClickListener { finish() }
        binding.btnCapture.setOnClickListener { viewModel.onCapture() }
        binding.btnRetake.setOnClickListener { viewModel.onRetake() }
        binding.btnContinue.setOnClickListener { viewModel.onContinue() }

        viewModel.uiState.observe(this) { state ->
            applyUiState(state ?: SelfieUiState.CAPTURE)
        }
        viewModel.navigateToPending.observe(this) { go ->
            if (go == true) {
                KycNavigator.openAfterSubmission(this)
            }
        }
    }

    private fun applyUiState(state: SelfieUiState) {
        val isCapture = state == SelfieUiState.CAPTURE
        binding.captureHints.visibility = if (isCapture) View.VISIBLE else View.GONE
        binding.btnCapture.visibility = if (isCapture) View.VISIBLE else View.GONE
        binding.reviewStatus.visibility = if (isCapture) View.GONE else View.VISIBLE
        binding.btnContinue.visibility = if (isCapture) View.GONE else View.VISIBLE
        binding.btnRetake.visibility = if (isCapture) View.GONE else View.VISIBLE
    }
}
