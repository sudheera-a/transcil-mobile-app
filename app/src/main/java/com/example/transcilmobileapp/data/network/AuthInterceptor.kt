package com.example.transcilmobileapp.data.network

import com.example.transcilmobileapp.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = TokenStore.getAccessToken()

        // No token yet (e.g. before login) -> send request as-is
        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }

        // Token exists -> attach Authorization header
        val requestWithAuth = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(requestWithAuth)
    }
}