package com.example.transcilmobileapp.kyc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityAadhaarVerificationBinding

import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.KycNavigator
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.core.UiFormHelpers

class AadhaarVerificationActivity :
    BaseActivity<ActivityAadhaarVerificationBinding>(ActivityAadhaarVerificationBinding::inflate) {

    private val viewModel: AadhaarVerificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UiFormHelpers.bindStepProgress(binding.stepProgress, activeStep = 3)
        UiFormHelpers.bindFocusHighlight(binding.etAadhaar)

        binding.ivBack.setOnClickListener { finish() }
        binding.tvSkip.setOnClickListener { viewModel.onSkipClicked() }
        binding.cbConsent.setOnCheckedChangeListener { _, checked ->
            viewModel.onConsentChanged(checked)
        }
        binding.btnVerify.setOnClickListener {
            viewModel.onVerifyClicked(binding.etAadhaar.text.toString())
        }

        viewModel.navigateToOtp.observe(this) { go ->
            if (go == true) {
                startActivity(Intent(this, AadhaarOtpActivity::class.java))
            }
        }
        viewModel.skipFlow.observe(this) { skip ->
            if (skip == true) {
                KycNavigator.openHomeDashboard(this, KycStatus.PENDING)
                finish()
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
