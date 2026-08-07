/**
 * Celebration screen shown when KYC is fully approved — single CTA to open the home dashboard.
 * Rider reaches here via KycNavigator after server reports documents.verified == true.
 */
package com.transcil.rider.kyc

import android.os.Bundle
import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.KycNavigator
import com.transcil.rider.core.KycStatus
import com.transcil.rider.databinding.ActivityKycApprovedBinding

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
