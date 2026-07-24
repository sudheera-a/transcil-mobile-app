package com.example.transcilmobileapp.repository

import com.example.transcilmobileapp.data.model.auth.*
import com.example.transcilmobileapp.data.network.TranscilApi
import java.util.UUID

class AuthRepository(
    private val api: TranscilApi = com.example.transcilmobileapp.data.network.ApiClient.transcilApi,
) {
    companion object {
        fun toE164(tenDigitMobile: String): String {
            val digits = tenDigitMobile.filter { it.isDigit() }.takeLast(10)
            return "+91$digits"
        }
    }

    suspend fun start(tenDigitMobile: String): Result<AuthStartData> = runCatching {
        val res = api.authStart(UUID.randomUUID().toString(), AuthStartRequest(toE164(tenDigitMobile)))
        res.error?.let { error(it.message ?: it.code ?: "AUTH_START_FAILED") }
        res.data ?: error("AUTH_START_EMPTY")
    }

    suspend fun verify(session: String, tenDigitMobile: String, otp: String): Result<AuthTokensData> =
        runCatching {
            val res = api.authVerify(
                UUID.randomUUID().toString(),
                AuthVerifyRequest(session, toE164(tenDigitMobile), otp)
            )
            res.error?.let { error(it.message ?: it.code ?: "AUTH_VERIFY_FAILED") }
            res.data ?: error("AUTH_VERIFY_EMPTY")
        }

    suspend fun refresh(refreshToken: String): Result<AuthTokensData> = runCatching {
        val res = api.authRefresh(UUID.randomUUID().toString(), AuthRefreshRequest(refreshToken))
        res.error?.let { error(it.message ?: it.code ?: "AUTH_REFRESH_FAILED") }
        res.data ?: error("AUTH_REFRESH_EMPTY")
    }

    suspend fun logout(): Result<Unit> = runCatching {
        val res = api.authLogout()
        res.error?.let { error(it.message ?: it.code ?: "AUTH_LOGOUT_FAILED") }
        Unit
    }
}
