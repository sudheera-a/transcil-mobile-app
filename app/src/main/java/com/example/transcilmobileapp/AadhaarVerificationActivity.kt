package com.example.transcilmobileapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityAadhaarVerificationBinding

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
                Toast.makeText(this, R.string.aadhaar_skipped_stub, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
