package com.example.transcilmobileapp.kyc

import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object DigioLauncher {
    fun open(activity: Activity, gatewayUrl: String) {
        val uri = Uri.parse(gatewayUrl)
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(activity, uri)
    }
}
