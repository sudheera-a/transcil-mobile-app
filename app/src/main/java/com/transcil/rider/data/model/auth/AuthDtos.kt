/**
 * Request/response bodies for phone OTP auth: start session, verify OTP, refresh tokens, logout.
 * Used by [TranscilApi] and [AuthRepository]; `@SerializedName` aligns Kotlin names with JSON keys.
 *
 * Kotlin notes:
 * - Separate Request vs Data types: outgoing POST body vs incoming `data` payload in [ApiResponse].
 * - Default `= null` on optional fields lets Gson omit or accept missing JSON properties.
 */
package com.transcil.rider.data.model.auth

import com.google.gson.annotations.SerializedName

data class AuthStartRequest(
    @SerializedName("phone_e164") val phoneE164: String,
)

data class AuthStartData(
    val session: String,
    @SerializedName("ttl_seconds") val ttlSeconds: Int? = null,
    @SerializedName("resend_after_seconds") val resendAfterSeconds: Int? = null,
    @SerializedName("phone_e164_masked") val phoneE164Masked: String? = null,
)

data class AuthVerifyRequest(
    val session: String,
    @SerializedName("phone_e164") val phoneE164: String,
    val otp: String,
)

data class AuthTokensData(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("id_token") val idToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null,
)

data class AuthRefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String,
)

data class AuthLogoutData(
    val ok: Boolean? = null,
)
