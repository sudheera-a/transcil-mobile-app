package com.example.transcilmobileapp.data.network

import com.example.transcilmobileapp.data.model.ApiResponse
import com.example.transcilmobileapp.data.model.HelpCenterDto
import com.example.transcilmobileapp.data.model.HtmlDocumentDto
import com.example.transcilmobileapp.data.model.auth.*
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

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
}
