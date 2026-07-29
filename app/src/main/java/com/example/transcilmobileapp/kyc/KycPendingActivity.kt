package com.example.transcilmobileapp.kyc

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.KycNavigator
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.databinding.ActivityKycPendingBinding
import com.example.transcilmobileapp.repository.OnboardingRepository
import kotlinx.coroutines.launch

class KycPendingActivity :
    BaseActivity<ActivityKycPendingBinding>(ActivityKycPendingBinding::inflate) {

    private val onboardingRepository = OnboardingRepository()

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

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            onboardingRepository.getOnboarding()
                .onSuccess { data ->
                    OnboardingSync.apply(data)
                    if (data.documents?.verified == true) {
                        KycNavigator.openForStatus(this@KycPendingActivity, KycStatus.APPROVED)
                        finish()
                    }
                }
        }
    }
}