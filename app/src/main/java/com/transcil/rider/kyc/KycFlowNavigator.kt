/**
 * Central navigation helper for the KYC sub-flow — returns riders to the accordion progress hub.
 * Uses CLEAR_TOP so back stack does not accumulate one Activity per step.
 */
package com.transcil.rider.kyc

import android.app.Activity
import android.content.Context
import android.content.Intent

/** `object`: stateless navigation utility — no instance needed. */
object KycFlowNavigator {

    fun openProgress(context: Context) {
        val intent = Intent(context, KycProgressActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
        if (context is Activity) {
            context.finish()
        }
    }
}
