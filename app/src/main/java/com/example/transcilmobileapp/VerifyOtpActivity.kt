package com.example.transcilmobileapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityVerifyOtpBinding

class VerifyOtpActivity : BaseActivity<ActivityVerifyOtpBinding>(ActivityVerifyOtpBinding::inflate) {

    private val viewModel: VerifyOtpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mobileNumber = intent.getStringExtra("MOBILE_NUMBER") ?: ""
        binding.tvOtpSentTo.text = "${getString(R.string.otp_sent_prefix)} +91 $mobileNumber"

        val otpBoxes = listOf(
            binding.etOtp1, binding.etOtp2, binding.etOtp3,
            binding.etOtp4, binding.etOtp5, binding.etOtp6
        )

        setupAutoAdvance(otpBoxes)

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnVerify.setOnClickListener {
            val otp = otpBoxes.joinToString("") { it.text.toString() }
            viewModel.onVerifyClicked(otp)
        }

        viewModel.navigateToHome.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                startActivity(Intent(this, ChooseJourneyActivity::class.java))
                finish()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAutoAdvance(boxes: List<EditText>) {
        for (i in boxes.indices) {
            boxes[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < boxes.size - 1) {
                        boxes[i + 1].requestFocus()
                    }
                }
            })
        }
    }
}