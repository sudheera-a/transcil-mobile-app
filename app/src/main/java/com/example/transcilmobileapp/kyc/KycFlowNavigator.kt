package com.example.transcilmobileapp.kyc

import android.app.Activity
import android.content.Context
import android.content.Intent

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
