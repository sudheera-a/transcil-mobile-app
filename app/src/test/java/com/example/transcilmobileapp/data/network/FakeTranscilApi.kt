package com.example.transcilmobileapp.data.network

import com.example.transcilmobileapp.data.model.ApiResponse
import com.example.transcilmobileapp.data.model.HelpCenterDto
import com.example.transcilmobileapp.data.model.HtmlDocumentDto
import com.example.transcilmobileapp.data.model.auth.*
import com.example.transcilmobileapp.data.model.kyc.DigioStartData
import com.example.transcilmobileapp.data.model.kyc.DigioStartRequest
import com.example.transcilmobileapp.data.model.kyc.DigioStatusData
import com.example.transcilmobileapp.data.model.onboarding.*
import com.google.gson.JsonObject

/** Default unused stubs so tests only override what they need. */
open class FakeTranscilApi : TranscilApi {
    override suspend fun getSettings(): ApiResponse<JsonObject> = unused()
    override suspend fun getTerms(): ApiResponse<HtmlDocumentDto> = unused()
    override suspend fun getPrivacy(): ApiResponse<HtmlDocumentDto> = unused()
    override suspend fun getHelpCenter(): ApiResponse<HelpCenterDto> = unused()
    override suspend fun getAds(): ApiResponse<JsonObject> = unused()
    override suspend fun getReturnGuidance(): ApiResponse<JsonObject> = unused()
    override suspend fun getRiderPrograms3pl(): ApiResponse<JsonObject> = unused()
    override suspend fun authStart(
        idempotencyKey: String,
        body: AuthStartRequest,
    ): ApiResponse<AuthStartData> = unused()
    override suspend fun authVerify(
        idempotencyKey: String,
        body: AuthVerifyRequest,
    ): ApiResponse<AuthTokensData> = unused()
    override suspend fun authRefresh(
        idempotencyKey: String,
        body: AuthRefreshRequest,
    ): ApiResponse<AuthTokensData> = unused()
    override suspend fun authLogout(): ApiResponse<AuthLogoutData> = unused()
    override suspend fun kycStart(
        idempotencyKey: String,
        body: DigioStartRequest,
    ): ApiResponse<DigioStartData> = unused()
    override suspend fun kycSyncStatus(
        idempotencyKey: String,
    ): ApiResponse<DigioStatusData> = unused()
    override suspend fun kycStatus(): ApiResponse<DigioStatusData> = unused()
    override suspend fun getJourneyOptions(): ApiResponse<List<JourneyOptionDto>> = unused()
    override suspend fun setRiderRole(body: RiderRoleRequest): ApiResponse<RiderRoleData> = unused()
    override suspend fun getProfile(): ApiResponse<ProfileData> = unused()
    override suspend fun patchProfile(
        idempotencyKey: String,
        body: ProfilePatchRequest,
    ): ApiResponse<ProfileData> = unused()
    override suspend fun getAddress(): ApiResponse<AddressData> = unused()
    override suspend fun putAddress(body: AddressUpsertRequest): ApiResponse<AddressData> = unused()
    override suspend fun getOnboarding(): ApiResponse<OnboardingData> = unused()
    override suspend fun getStates(): ApiResponse<List<ReferenceStateDto>> = unused()
    override suspend fun getCities(stateCode: String): ApiResponse<List<ReferenceCityDto>> = unused()

    private fun <T> unused(): T = error("unused")
}
