package com.example.transcilmobileapp.kyc

import android.os.Bundle
import android.widget.Toast
import com.example.transcilmobileapp.databinding.ActivityKycApprovedBinding

import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity

class KycApprovedActivity :
    BaseActivity<ActivityKycApprovedBinding>(ActivityKycApprovedBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnContinue.setOnClickListener {
            Toast.makeText(this, R.string.dashboard_coming_soon, Toast.LENGTH_SHORT).show()
        }
    }
}
