/**
 * Standalone bank-details screen — collects account holder, number, IFSC for KYC bank step.
 * Validates locally, saves draft to KycProgressRepository, returns to progress hub on success.
 */
package com.transcil.rider.kyc

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.transcil.rider.databinding.ActivityBankDetailsBinding

import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.KycNavigator
import com.transcil.rider.core.KycStatus
import com.transcil.rider.core.UiFormHelpers

class BankDetailsActivity :
    BaseActivity<ActivityBankDetailsBinding>(ActivityBankDetailsBinding::inflate) {

    private val viewModel: BankDetailsViewModel by viewModels()

    /** `override fun`: wire form fields to BankDetailsViewModel; observe navigation events. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UiFormHelpers.bindStepProgress(binding.stepProgress, activeStep = 4)
        UiFormHelpers.bindFocusHighlight(binding.etHolderName)
        UiFormHelpers.bindFocusHighlight(binding.etAccountNumber)
        UiFormHelpers.bindFocusHighlight(binding.etConfirmAccountNumber)
        UiFormHelpers.bindFocusHighlight(binding.etIfsc)

        binding.ivBack.setOnClickListener { finish() }
        binding.tvSkip.setOnClickListener { viewModel.onSkipClicked() }
        binding.btnVerify.setOnClickListener {
            viewModel.onVerifyClicked(
                holderName = binding.etHolderName.text.toString(),
                accountNumber = binding.etAccountNumber.text.toString(),
                confirmAccountNumber = binding.etConfirmAccountNumber.text.toString(),
                ifsc = binding.etIfsc.text.toString()
            )
        }

        viewModel.navigateNext.observe(this) { go ->
            if (go == true) {
                KycFlowNavigator.openProgress(this)
            }
        }
        viewModel.skipToHome.observe(this) { skip ->
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
