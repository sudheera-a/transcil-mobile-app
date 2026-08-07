/**
 * Repository for Digio-hosted KYC: start a gateway session and sync completion status after redirect.
 * Used when onboarding routes the rider through external Digio verification instead of manual upload.
 *
 * Kotlin notes:
 * - `companion object` holds [DIGIO_REDIRECT_URL] deep link the app registers for callback Activities.
 * - `suspend fun` + [Result] same pattern as [AuthRepository].
 */
package com.transcil.rider.repository

import com.transcil.rider.data.model.kyc.DigioStartData
import com.transcil.rider.data.model.kyc.DigioStartRequest
import com.transcil.rider.data.model.kyc.DigioStatusData
import com.transcil.rider.data.network.TranscilApi
import java.util.UUID

class DigioKycRepository(
    private val api: TranscilApi = com.transcil.rider.data.network.ApiClient.transcilApi,
) {
    companion object {
        const val DIGIO_REDIRECT_URL = "transcil://kyc/callback"
    }

    suspend fun start(customerName: String): Result<DigioStartData> = runCatching {
        val name = customerName.trim()
        if (name.isBlank() || name.any { it.isDigit() }) {
            error("INVALID_CUSTOMER_NAME")
        }
        val res = api.kycStart(
            UUID.randomUUID().toString(),
            DigioStartRequest(customerName = name, redirectUrl = DIGIO_REDIRECT_URL),
        )
        res.error?.let { error(it.message ?: it.code ?: "KYC_START_FAILED") }
        res.data ?: error("KYC_START_EMPTY")
    }

    suspend fun syncStatus(): Result<DigioStatusData> = runCatching {
        val res = api.kycSyncStatus(UUID.randomUUID().toString())
        res.error?.let { error(it.message ?: it.code ?: "KYC_SYNC_FAILED") }
        res.data ?: error("KYC_SYNC_EMPTY")
    }
}
