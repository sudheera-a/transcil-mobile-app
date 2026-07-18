package com.example.transcilmobileapp.kyc

import android.os.Bundle
import android.widget.Toast
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.KycNavigator
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.databinding.ActivityKycPendingBinding

class KycPendingActivity :
    BaseActivity<ActivityKycPendingBinding>(ActivityKycPendingBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnReviewProgress.isEnabled = true
        binding.btnReviewProgress.setText(R.string.kyc_go_home)
        binding.btnReviewProgress.setOnClickListener {
            KycNavigator.openHomeDashboard(this, KycStatus.PENDING)
            finish()
        }
        binding.tvContactSupport.setOnClickListener {
            Toast.makeText(this, R.string.kyc_support_stub, Toast.LENGTH_SHORT).show()
        }
    }
}
