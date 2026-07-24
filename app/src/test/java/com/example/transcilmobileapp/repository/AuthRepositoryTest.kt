package com.example.transcilmobileapp.repository

import com.example.transcilmobileapp.data.model.ApiError
import com.example.transcilmobileapp.data.model.ApiResponse
import com.example.transcilmobileapp.data.model.HelpCenterDto
import com.example.transcilmobileapp.data.model.HtmlDocumentDto
import com.example.transcilmobileapp.data.model.auth.*
import com.example.transcilmobileapp.data.network.TranscilApi
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthRepositoryTest {

    private class FakeApi : TranscilApi {
        var lastStart: AuthStartRequest? = null
        var startResponse: ApiResponse<AuthStartData> = ApiResponse(
            AuthStartData(session = "S1"), null, null
        )
        var verifyResponse: ApiResponse<AuthTokensData> = ApiResponse(
            AuthTokensData(accessToken = "a", refreshToken = "r"), null, null
        )

        override suspend fun getSettings(): ApiResponse<JsonObject> =
            ApiResponse(null, null, null)

        override suspend fun getTerms(): ApiResponse<HtmlDocumentDto> =
            ApiResponse(null, null, null)

        override suspend fun getPrivacy(): ApiResponse<HtmlDocumentDto> =
            ApiResponse(null, null, null)

        override suspend fun getHelpCenter(): ApiResponse<HelpCenterDto> =
            ApiResponse(null, null, null)

        override suspend fun getAds(): ApiResponse<JsonObject> =
            ApiResponse(null, null, null)

        override suspend fun getReturnGuidance(): ApiResponse<JsonObject> =
            ApiResponse(null, null, null)

        override suspend fun getRiderPrograms3pl(): ApiResponse<JsonObject> =
            ApiResponse(null, null, null)

        override suspend fun authStart(
            idempotencyKey: String,
            body: AuthStartRequest,
        ): ApiResponse<AuthStartData> {
            lastStart = body
            return startResponse
        }

        override suspend fun authVerify(
            idempotencyKey: String,
            body: AuthVerifyRequest,
        ): ApiResponse<AuthTokensData> = verifyResponse

        override suspend fun authRefresh(
            idempotencyKey: String,
            body: AuthRefreshRequest,
        ): ApiResponse<AuthTokensData> = error("unused")

        override suspend fun authLogout(): ApiResponse<AuthLogoutData> = error("unused")
    }

    @Test
    fun toE164_prefixesIndia() {
        assertEquals("+919876543210", AuthRepository.toE164("9876543210"))
    }

    @Test
    fun start_sendsE164() = runBlocking {
        val api = FakeApi()
        val repo = AuthRepository(api)
        val result = repo.start("9876543210")
        assertTrue(result.isSuccess)
        assertEquals("+919876543210", api.lastStart?.phoneE164)
        assertEquals("S1", result.getOrNull()?.session)
    }

    @Test
    fun verify_mapsApiError() = runBlocking {
        val api = FakeApi().apply {
            verifyResponse = ApiResponse(
                null, null,
                ApiError(code = "AUTH_OTP_INVALID", message = "Incorrect OTP", clientAction = "stay_on_screen")
            )
        }
        val result = AuthRepository(api).verify("S1", "9876543210", "000000")
        assertTrue(result.isFailure)
        assertEquals("Incorrect OTP", result.exceptionOrNull()?.message)
    }
}
