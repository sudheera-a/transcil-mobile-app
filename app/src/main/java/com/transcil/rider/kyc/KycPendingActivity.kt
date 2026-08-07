/**
 * Shown when KYC documents are under server review — polls onboarding on resume for approval.
 * Offers navigation home with PENDING status; auto-redirects to approved flow when verified.
 */
package com.transcil.rider.kyc

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.transcil.rider.R
import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.KycNavigator
import com.transcil.rider.core.KycStatus
import com.transcil.rider.databinding.ActivityKycPendingBinding
import com.transcil.rider.repository.OnboardingRepository
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

    /** `override fun`: re-fetch onboarding when user returns — server may have approved while away. */
    override fun onResume() {
        super.onResume()
        // lifecycleScope.launch: coroutine cancelled automatically when Activity is destroyed.
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