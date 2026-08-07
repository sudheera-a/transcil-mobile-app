/**
 * OkHttp interceptor: before each request goes out, add `Authorization: Bearer <token>`
 * if [TokenStore] has an access token.
 *
 * Interceptor vs Authenticator:
 * - Interceptor = runs on every request (attach header).
 * - Authenticator = runs only after a 401 (refresh + retry) — see [TokenAuthenticator].
 *
 * `override fun intercept` = required by OkHttp's Interceptor interface.
 */
package com.transcil.rider.data.network

import com.transcil.rider.data.local.TokenStore
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
