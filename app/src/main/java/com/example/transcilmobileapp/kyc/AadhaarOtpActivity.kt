package com.example.transcilmobileapp.kyc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityAadhaarOtpBinding

import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.UiFormHelpers

class AadhaarOtpActivity :
    BaseActivity<ActivityAadhaarOtpBinding>(ActivityAadhaarOtpBinding::inflate) {

    private val viewModel: AadhaarOtpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UiFormHelpers.bindStepProgress(binding.stepProgress, activeStep = 3)
        val otpBoxes = listOf(
            binding.etOtp1, binding.etOtp2, binding.etOtp3,
            binding.etOtp4, binding.etOtp5, binding.etOtp6
        )
        UiFormHelpers.setupOtpAutoAdvance(otpBoxes)

        binding.ivBack.setOnClickListener { finish() }
        binding.tvResend.setOnClickListener { viewModel.onResendClicked() }
        binding.btnVerify.setOnClickListener {
            viewModel.onVerifyClicked(otpBoxes.joinToString("") { it.text.toString() })
        }

        viewModel.resendSeconds.observe(this) { seconds ->
            binding.tvResend.text = if (seconds > 0) {
                getString(R.string.resend_otp_in, seconds)
            } else {
                getString(R.string.resend_otp)
            }
        }
        viewModel.canResend.observe(this) { canResend ->
            binding.tvResend.isEnabled = canResend == true
            binding.tvResend.alpha = if (canResend == true) 1f else 0.85f
        }
        viewModel.navigateNext.observe(this) { go ->
            if (go == true) {
                startActivity(Intent(this, PanVerificationActivity::class.java))
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
