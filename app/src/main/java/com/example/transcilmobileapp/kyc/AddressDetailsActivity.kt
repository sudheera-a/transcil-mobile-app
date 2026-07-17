package com.example.transcilmobileapp.kyc

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityAddressDetailsBinding

import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.UiFormHelpers

class AddressDetailsActivity :
    BaseActivity<ActivityAddressDetailsBinding>(ActivityAddressDetailsBinding::inflate) {

    private val viewModel: AddressDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UiFormHelpers.bindStepProgress(binding.stepProgress, activeStep = 2)
        UiFormHelpers.bindFocusHighlight(binding.etAddressLine1)
        UiFormHelpers.bindFocusHighlight(binding.etAddressLine2)

        binding.ivBack.setOnClickListener { finish() }
        binding.btnContinue.setOnClickListener {
            viewModel.onContinueClicked(
                line1 = binding.etAddressLine1.text.toString(),
                line2 = binding.etAddressLine2.text.toString(),
                city = binding.etCity.text.toString(),
                state = binding.etState.text.toString(),
                pincode = binding.etPincode.text.toString()
            )
        }

        viewModel.navigateNext.observe(this) { go ->
            if (go == true) {
                KycProgressRepository.markCompleted(KycStep.ADDRESS)
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
