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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

interface TranscilApi {

    @GET("v1/settings")
    suspend fun getSettings(): ApiResponse<JsonObject>

    @GET("v1/terms")
    suspend fun getTerms(): ApiResponse<HtmlDocumentDto>

    @GET("v1/privacy")
    suspend fun getPrivacy(): ApiResponse<HtmlDocumentDto>

    @GET("v1/help-center")
    suspend fun getHelpCenter(): ApiResponse<HelpCenterDto>

    @GET("v1/ads")
    suspend fun getAds(): ApiResponse<JsonObject>

    @GET("v1/return-guidance")
    suspend fun getReturnGuidance(): ApiResponse<JsonObject>

    @GET("v1/rider-programs/3pl")
    suspend fun getRiderPrograms3pl(): ApiResponse<JsonObject>

    @POST("v1/auth/start")
    suspend fun authStart(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: AuthStartRequest,
    ): ApiResponse<AuthStartData>

    @POST("v1/auth/verify")
    suspend fun authVerify(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: AuthVerifyRequest,
    ): ApiResponse<AuthTokensData>

    @POST("v1/auth/refresh")
    suspend fun authRefresh(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: AuthRefreshRequest,
    ): ApiResponse<AuthTokensData>

    @POST("v1/auth/logout")
    suspend fun authLogout(): ApiResponse<AuthLogoutData>

    @POST("v1/kyc/start")
    suspend fun kycStart(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: DigioStartRequest,
    ): ApiResponse<DigioStartData>

    @POST("v1/kyc/sync-status")
    suspend fun kycSyncStatus(
        @Header("Idempotency-Key") idempotencyKey: String,
    ): ApiResponse<DigioStatusData>

    @GET("v1/kyc/status")
    suspend fun kycStatus(): ApiResponse<DigioStatusData>

    @GET("v1/onboarding/journey-options")
    suspend fun getJourneyOptions(): ApiResponse<List<JourneyOptionDto>>

    @PUT("v1/me/rider-role")
    suspend fun setRiderRole(
        @Body body: RiderRoleRequest,
    ): ApiResponse<RiderRoleData>

    @GET("v1/me/profile")
    suspend fun getProfile(): ApiResponse<ProfileData>

    @PATCH("v1/me/profile")
    suspend fun patchProfile(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: ProfilePatchRequest,
    ): ApiResponse<ProfileData>

    @GET("v1/me/address")
    suspend fun getAddress(): ApiResponse<AddressData>

    @PUT("v1/me/address")
    suspend fun putAddress(
        @Body body: AddressUpsertRequest,
    ): ApiResponse<AddressData>

    @GET("v1/me/onboarding")
    suspend fun getOnboarding(): ApiResponse<OnboardingData>

    @GET("v1/reference/states")
    suspend fun getStates(): ApiResponse<List<ReferenceStateDto>>

    @GET("v1/reference/states/{stateCode}/cities")
    suspend fun getCities(
        @retrofit2.http.Path("stateCode") stateCode: String,
    ): ApiResponse<List<ReferenceCityDto>>
}
