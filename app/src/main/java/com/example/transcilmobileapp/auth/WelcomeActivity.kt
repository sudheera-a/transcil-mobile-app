package com.example.transcilmobileapp.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.FeedbackUi
import com.example.transcilmobileapp.core.NavExtras
import com.example.transcilmobileapp.core.SegmentedStepper
import com.example.transcilmobileapp.databinding.ActivityWelcomeBinding

class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>(ActivityWelcomeBinding::inflate) {

    private val viewModel: WelcomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.navyHeaderInclude.findViewById<TextView>(R.id.headerTitle).setText(R.string.welcome_title)
        binding.navyHeaderInclude.findViewById<TextView>(R.id.headerSubtitle).setText(R.string.welcome_subtitle)
        SegmentedStepper.apply(binding.navyHeaderInclude, filledCount = 1, navyInactive = true)

        binding.btnSendOtp.setOnClickListener {
            viewModel.onSendOtpClicked(binding.etMobileNumber.text.toString())
        }
        binding.ivBack.setOnClickListener { finish() }

        viewModel.navigateToVerifyOtp.observe(this) { event ->
            startActivity(
                Intent(this, VerifyOtpActivity::class.java)
                    .putExtra(NavExtras.MOBILE_NUMBER, event.mobile)
                    .putExtra(NavExtras.OTP_SESSION, event.session),
            )
        }
        viewModel.isLoading.observe(this) { loading ->
            binding.btnSendOtp.isEnabled = loading != true
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) FeedbackUi.toast(this, message)
        }
    }
}
