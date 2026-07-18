package com.example.transcilmobileapp.kyc

import android.os.Bundle
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.KycNavigator
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.databinding.ActivityKycApprovedBinding

class KycApprovedActivity :
    BaseActivity<ActivityKycApprovedBinding>(ActivityKycApprovedBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnContinue.setOnClickListener {
            KycNavigator.openHomeDashboard(this, KycStatus.APPROVED)
            finish()
        }
    }
}
