package com.example.transcilmobileapp.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityVerifyOtpBinding

import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.NavExtras
import com.example.transcilmobileapp.core.UiFormHelpers
import com.example.transcilmobileapp.journey.ChooseJourneyActivity

class VerifyOtpActivity : BaseActivity<ActivityVerifyOtpBinding>(ActivityVerifyOtpBinding::inflate) {

    private val viewModel: VerifyOtpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mobileNumber = intent.getStringExtra(NavExtras.MOBILE_NUMBER).orEmpty()
        binding.tvOtpSentTo.text = "${getString(R.string.otp_sent_prefix)} +91 $mobileNumber"

        val otpBoxes = listOf(
            binding.etOtp1, binding.etOtp2, binding.etOtp3,
            binding.etOtp4, binding.etOtp5, binding.etOtp6
        )
        UiFormHelpers.setupOtpAutoAdvance(otpBoxes)

        binding.ivBack.setOnClickListener { finish() }
        binding.btnVerify.setOnClickListener {
            viewModel.onVerifyClicked(otpBoxes.joinToString("") { it.text.toString() })
        }

        viewModel.navigateToHome.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                startActivity(Intent(this, ChooseJourneyActivity::class.java))
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
