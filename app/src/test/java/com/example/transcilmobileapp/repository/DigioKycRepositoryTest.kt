package com.example.transcilmobileapp.repository

import com.example.transcilmobileapp.data.model.ApiResponse
import com.example.transcilmobileapp.data.model.kyc.DigioStartData
import com.example.transcilmobileapp.data.model.kyc.DigioStartRequest
import com.example.transcilmobileapp.data.model.kyc.DigioStatusData
import com.example.transcilmobileapp.data.network.FakeTranscilApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DigioKycRepositoryTest {

    private class FakeApi : FakeTranscilApi() {
        var lastStart: DigioStartRequest? = null
        var startCalled = false
        var startResponse: ApiResponse<DigioStartData> = ApiResponse(
            DigioStartData(gatewayUrl = "https://gateway.example/start"),
            null,
            null,
        )
        var syncResponse: ApiResponse<DigioStatusData> = ApiResponse(
            DigioStatusData(status = "pending"),
            null,
            null,
        )

        override suspend fun kycStart(
            idempotencyKey: String,
            body: DigioStartRequest,
        ): ApiResponse<DigioStartData> {
            startCalled = true
            lastStart = body
            return startResponse
        }

        override suspend fun kycSyncStatus(
            idempotencyKey: String,
        ): ApiResponse<DigioStatusData> = syncResponse
    }

    @Test
    fun start_sendsRedirectAndName() = runBlocking {
        val fake = FakeApi()
        val repo = DigioKycRepository(fake)
        val result = repo.start("Ravi Kumar")
        assertTrue(result.isSuccess)
        assertEquals("transcil://kyc/callback", fake.lastStart?.redirectUrl)
        assertEquals("Ravi Kumar", fake.lastStart?.customerName)
    }

    @Test
    fun start_rejectsNameWithDigits() = runBlocking {
        val fake = FakeApi()
        val repo = DigioKycRepository(fake)
        val result = repo.start("Ravi 123")
        assertTrue(result.isFailure)
        assertFalse(fake.startCalled)
    }

    @Test
    fun start_rejectsBlankName() = runBlocking {
        val fake = FakeApi()
        val repo = DigioKycRepository(fake)
        val result = repo.start("   ")
        assertTrue(result.isFailure)
        assertFalse(fake.startCalled)
    }
}
