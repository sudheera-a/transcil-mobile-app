package com.example.transcilmobileapp.kyc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityPanVerificationBinding

import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.UiFormHelpers

class PanVerificationActivity :
    BaseActivity<ActivityPanVerificationBinding>(ActivityPanVerificationBinding::inflate) {

    private val viewModel: PanVerificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UiFormHelpers.bindStepProgress(binding.stepProgress, activeStep = 4)
        UiFormHelpers.bindFocusHighlight(binding.etPan)

        binding.ivBack.setOnClickListener { finish() }
        binding.tvSkip.setOnClickListener { viewModel.onSkipClicked() }
        binding.tvSkipForNow.setOnClickListener { viewModel.onSkipClicked() }
        binding.btnVerify.setOnClickListener {
            viewModel.onVerifyClicked(binding.etPan.text.toString())
        }

        viewModel.navigateNext.observe(this) { go ->
            if (go == true) {
                startActivity(Intent(this, BankDetailsActivity::class.java))
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
