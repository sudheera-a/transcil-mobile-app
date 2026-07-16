package com.example.transcilmobileapp.kyc

import android.os.Bundle
import android.widget.Toast
import com.example.transcilmobileapp.databinding.ActivityKycPendingBinding

import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity

class KycPendingActivity :
    BaseActivity<ActivityKycPendingBinding>(ActivityKycPendingBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.tvContactSupport.setOnClickListener {
            Toast.makeText(this, R.string.kyc_support_stub, Toast.LENGTH_SHORT).show()
        }
    }
}
