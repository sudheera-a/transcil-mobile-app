package com.example.transcilmobileapp.data.network

import com.example.transcilmobileapp.data.local.TokenStore
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Runs only when server returns 401.
 *
 * Learning version:
 * - Prevents infinite retry loops
 * - Clears tokens if we cannot refresh
 *
 * Later (when backend gives refresh API):
 * - Call refresh endpoint with refreshToken
 * - Save new accessToken
 * - Retry original request with new Bearer header
 */
class TokenAuthenticator : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 1) Already retried? Give up to avoid infinite loop
        if (responseCount(response) >= 2) {
            TokenStore.clear()
            return null
        }

        // 2) If the failed call was refresh itself, don't loop
        val path = response.request.url.encodedPath
        if (path.contains("auth/refresh") || path.contains("token/refresh")) {
            TokenStore.clear()
            return null
        }

        // 3) No refresh token available yet -> cannot recover
        val refreshToken = TokenStore.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            TokenStore.clear()
            return null // null = do not retry; caller sees 401
        }

        val newAccess = AuthTokenRefresher.refreshBlocking(refreshToken) ?: run {
            TokenStore.clear()
            return null
        }
        TokenStore.save(newAccess, refreshToken)
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .build()
    }

    /**
     * Counts how many times this request was already tried.
     * Stops endless 401 -> authenticate -> 401 loops.
     */
    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}