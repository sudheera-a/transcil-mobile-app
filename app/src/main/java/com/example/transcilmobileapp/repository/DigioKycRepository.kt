package com.example.transcilmobileapp.repository

import com.example.transcilmobileapp.data.model.kyc.DigioStartData
import com.example.transcilmobileapp.data.model.kyc.DigioStartRequest
import com.example.transcilmobileapp.data.model.kyc.DigioStatusData
import com.example.transcilmobileapp.data.network.TranscilApi
import java.util.UUID

class DigioKycRepository(
    private val api: TranscilApi = com.example.transcilmobileapp.data.network.ApiClient.transcilApi,
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
