package com.example.transcilmobileapp.data.network

import com.example.transcilmobileapp.BuildConfig
import com.example.transcilmobileapp.data.model.ApiResponse
import com.example.transcilmobileapp.data.model.auth.AuthTokensData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

/**
 * Synchronous refresh via plain OkHttp (no Authenticator — avoids 401 refresh loops).
 */
object AuthTokenRefresher {

    /** Test-only override; null → [BuildConfig.BASE_URL]. */
    @Volatile
    var baseUrlOverride: String? = null

    private val client = OkHttpClient.Builder().build()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val responseType = object : TypeToken<ApiResponse<AuthTokensData>>() {}.type

    fun refreshBlocking(refreshToken: String): String? {
        if (refreshToken.isBlank()) return null

        val base = (baseUrlOverride ?: BuildConfig.BASE_URL).let {
            if (it.endsWith("/")) it else "$it/"
        }
        val url = "${base}v1/auth/refresh"
        val body = gson.toJson(mapOf("refresh_token" to refreshToken))
            .toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val raw = response.body?.string() ?: return null
                val parsed = gson.fromJson<ApiResponse<AuthTokensData>>(raw, responseType)
                if (parsed.error != null) return null
                parsed.data?.accessToken?.takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }
}
