package com.example.transcilmobileapp.core

import android.content.Context
import android.content.Intent
import com.example.transcilmobileapp.home.HomeDashboardActivity
import com.example.transcilmobileapp.kyc.KycApprovedActivity
import com.example.transcilmobileapp.kyc.KycPendingActivity

/**
 * Central navigation for post-submission KYC status screens and home dashboard.
 * Pending/Approved are driven by onboarding `documents.*`.
 */
object KycNavigator {

    const val EXTRA_KYC_STATUS = "extra_kyc_status"

    fun openAfterSubmission(
        context: Context,
        documentsVerified: Boolean = false,
        documentsOverall: String? = null,
    ) {
        val status = when {
            documentsVerified -> KycStatus.APPROVED
            documentsOverall.equals("in_progress", ignoreCase = true) -> KycStatus.PENDING
            else -> KycStatus.PENDING
        }
        openForStatus(context, status)
    }

    fun openForStatus(context: Context, status: KycStatus) {
        val target = when (status) {
            KycStatus.PENDING -> KycPendingActivity::class.java
            KycStatus.APPROVED -> KycApprovedActivity::class.java
        }
        context.startActivity(
            Intent(context, target).putExtra(EXTRA_KYC_STATUS, status.name)
        )
    }

    /**
     * Opens the post-KYC shell as the task root so system Back cannot return
     * to KYC/onboarding screens underneath.
     */
    fun openHomeDashboard(context: Context, status: KycStatus) {
        val intent = HomeDashboardActivity.createIntent(context, status).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
