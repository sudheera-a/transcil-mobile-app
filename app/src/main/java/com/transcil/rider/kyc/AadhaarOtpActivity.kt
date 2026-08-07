/**
 * Aadhaar OTP entry screen (6-digit boxes with auto-advance). Legacy path — Digio e-KYC is preferred.
 * On successful verify, returns to KycProgressActivity without marking the step complete locally.
 */
package com.transcil.rider.kyc

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.transcil.rider.databinding.ActivityAadhaarOtpBinding

import com.transcil.rider.R
import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.UiFormHelpers

class AadhaarOtpActivity :
    BaseActivity<ActivityAadhaarOtpBinding>(ActivityAadhaarOtpBinding::inflate) {

    private val viewModel: AadhaarOtpViewModel by viewModels()

    /** `override fun`: inflate ViewBinding, wire OTP UI, observe ViewModel one-shot events. */
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
                // Direct Aadhaar OTP is not the Phase C happy path; never local-complete.
                KycFlowNavigator.openProgress(this)
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
