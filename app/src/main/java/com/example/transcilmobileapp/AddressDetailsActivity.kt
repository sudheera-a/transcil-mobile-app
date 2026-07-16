package com.example.transcilmobileapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityAddressDetailsBinding

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
                startActivity(Intent(this, AadhaarVerificationActivity::class.java))
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
