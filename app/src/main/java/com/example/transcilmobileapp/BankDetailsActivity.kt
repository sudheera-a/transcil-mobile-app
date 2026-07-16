package com.example.transcilmobileapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityBankDetailsBinding

class BankDetailsActivity :
    BaseActivity<ActivityBankDetailsBinding>(ActivityBankDetailsBinding::inflate) {

    private val viewModel: BankDetailsViewModel by viewModels()

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
                Toast.makeText(this, R.string.onboarding_complete_stub, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
