/**
 * DTOs for Digio KYC gateway integration: start a verification session and poll sync status.
 * [DigioKycRepository] maps these to Activities that launch the external Digio WebView/SDK flow.
 *
 * Kotlin notes:
 * - `@SerializedName` on snake_case API fields (e.g. gateway_url → gatewayUrl).
 */
package com.transcil.rider.data.model.kyc

import com.google.gson.annotations.SerializedName

data class DigioStartRequest(
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("redirect_url") val redirectUrl: String,
)

data class DigioStartData(
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("digio_request_id") val digioRequestId: String? = null,
    val status: String? = null,
    @SerializedName("gateway_url") val gatewayUrl: String,
)

data class DigioStatusData(
    val status: String,
    @SerializedName("digio_request_id") val digioRequestId: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("onboarding_required") val onboardingRequired: Boolean? = null,
    @SerializedName("completed_at") val completedAt: String? = null,
)
