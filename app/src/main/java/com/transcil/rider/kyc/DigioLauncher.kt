/**
 * Opens Digio e-KYC gateway URL in a Chrome Custom Tab (in-app browser with shared cookies).
 * Keeps the rider inside the app shell while completing Aadhaar verification on Digio's site.
 */
package com.transcil.rider.kyc

import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/** `object`: thin wrapper around CustomTabsIntent — no state to hold. */
object DigioLauncher {
    fun open(activity: Activity, gatewayUrl: String) {
        val uri = Uri.parse(gatewayUrl)
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(activity, uri)
    }
}
