/**
 * In-memory fake [TranscilApi] for unit tests that need controllable network responses.
 */
package com.transcil.rider.data.network

import com.transcil.rider.data.model.ApiResponse
import com.transcil.rider.data.model.HelpCenterDto
import com.transcil.rider.data.model.HtmlDocumentDto
import com.transcil.rider.data.model.auth.*
import com.transcil.rider.data.model.kyc.DigioStartData
import com.transcil.rider.data.model.kyc.DigioStartRequest
import com.transcil.rider.data.model.kyc.DigioStatusData
import com.transcil.rider.data.model.kyc.KycDocumentSummary
import com.transcil.rider.data.model.kyc.KycListData
import com.transcil.rider.data.model.kyc.KycSubmitRequest
import com.transcil.rider.data.model.kyc.KycUploadData
import com.transcil.rider.data.model.kyc.KycUploadRequest
import com.transcil.rider.data.model.kyc.PanVerifyData
import com.transcil.rider.data.model.kyc.PanVerifyRequest
import com.transcil.rider.data.model.kyc.ReferenceData
import com.transcil.rider.data.model.kyc.ReferenceUpsertRequest
import com.transcil.rider.data.model.onboarding.*
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
    override suspend fun putAddress(
        idempotencyKey: String,
        body: AddressUpsertRequest,
    ): ApiResponse<AddressData> = unused()
    override suspend fun getOnboarding(): ApiResponse<OnboardingData> = unused()
    override suspend fun getReference(): ApiResponse<ReferenceData> = unused()
    override suspend fun putReference(
        idempotencyKey: String,
        body: ReferenceUpsertRequest,
    ): ApiResponse<ReferenceData> = unused()
    override suspend fun kycUploadRequest(
        idempotencyKey: String,
        body: KycUploadRequest,
    ): ApiResponse<KycUploadData> = unused()
    override suspend fun kycSubmit(
        idempotencyKey: String,
        body: KycSubmitRequest,
    ): ApiResponse<KycDocumentSummary> = unused()
    override suspend fun listKyc(): ApiResponse<KycListData> = unused()
    override suspend fun verifyPan(
        idempotencyKey: String,
        body: PanVerifyRequest,
    ): ApiResponse<PanVerifyData> = unused()
    override suspend fun getStates(): ApiResponse<List<ReferenceStateDto>> = unused()
    override suspend fun getCities(stateCode: String): ApiResponse<List<ReferenceCityDto>> = unused()

    private fun <T> unused(): T = error("unused")
}
