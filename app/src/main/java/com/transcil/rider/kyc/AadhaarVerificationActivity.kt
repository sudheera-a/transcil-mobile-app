/**
 * Aadhaar number + consent screen that launches Digio e-KYC in a Chrome Custom Tab.
 * Validates 12-digit Aadhaar and rider name before requesting a Digio gateway URL from the server.
 */
package com.transcil.rider.kyc

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.transcil.rider.databinding.ActivityAadhaarVerificationBinding

import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.KycNavigator
import com.transcil.rider.core.KycStatus
import com.transcil.rider.core.UiFormHelpers

class AadhaarVerificationActivity :
    BaseActivity<ActivityAadhaarVerificationBinding>(ActivityAadhaarVerificationBinding::inflate) {

    private val viewModel: AadhaarVerificationViewModel by viewModels()

    /** `override fun`: bind consent checkbox and launch Digio when ViewModel emits gateway URL. */
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

        viewModel.openDigioUrl.observe(this) { url ->
            if (!url.isNullOrBlank()) {
                DigioLauncher.open(this, url)
                viewModel.clearOpenDigioUrl()
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
