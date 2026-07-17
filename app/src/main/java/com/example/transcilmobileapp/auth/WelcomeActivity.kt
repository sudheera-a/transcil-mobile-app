package com.example.transcilmobileapp.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityWelcomeBinding

import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.NavExtras

class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>(ActivityWelcomeBinding::inflate) {

    private val viewModel: WelcomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnSendOtp.setOnClickListener {
            val mobileNumber = binding.etMobileNumber.text.toString()
            viewModel.onSendOtpClicked(mobileNumber)
        }

        binding.ivBack.setOnClickListener {
            finish()
        }

        viewModel.navigateToVerifyOtp.observe(this) { mobileNumber ->
            val intent = Intent(this, VerifyOtpActivity::class.java)
            intent.putExtra(NavExtras.MOBILE_NUMBER, mobileNumber)
            startActivity(intent)
        }

        viewModel.errorMessage.observe(this) { message ->
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}