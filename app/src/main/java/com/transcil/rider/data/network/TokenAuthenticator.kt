/**
 * OkHttp hook: when the server returns HTTP 401 (unauthorized), try refreshing the
 * access token once, then retry the failed request with the new token.
 *
 * Kotlin / OkHttp notes:
 * - `override fun authenticate` = implements Authenticator; OkHttp calls this on 401.
 * - `private fun authorized` = builds a copy of the request with a new Bearer token header.
 *   `private` means only this class can call it (not UI / repositories).
 * - `companion object` = "static-like" members shared by all instances of this class
 *   (here: the lock + header string constants). Access like TokenAuthenticator's internals.
 * - Returning null from authenticate = give up (caller sees the 401 / we cleared tokens).
 */
package com.transcil.rider.data.network

import com.transcil.rider.data.local.TokenStore
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp [Authenticator] for HTTP 401: single-flight refresh, then retry once.
 *
 * Best-practice behavior:
 * - Never authenticate the refresh call itself (avoids loops)
 * - Cap retries via prior-response chain
 * - If another thread already refreshed, reuse the new access token
 * - Clear local tokens when recovery is impossible
 */
class TokenAuthenticator : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // If the refresh endpoint itself 401'd, clearing tokens avoids an infinite loop.
        if (isRefreshRequest(response.request)) {
            TokenStore.clear()
            return null
        }

        // One authenticator-driven retry only (original + 1).
        if (responseCount(response) >= 2) {
            TokenStore.clear()
            return null
        }

        val failedAccess = bearerToken(response.request)

        synchronized(lock) {
            val currentAccess = TokenStore.getAccessToken()
            // Another concurrent 401 already refreshed successfully.
            if (!currentAccess.isNullOrBlank() && currentAccess != failedAccess) {
                return authorized(response.request, currentAccess)
            }

            val refreshToken = TokenStore.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                TokenStore.clear()
                return null
            }

            val newAccess = AuthTokenRefresher.refreshBlocking(refreshToken)
            if (newAccess.isNullOrBlank()) {
                TokenStore.clear()
                return null
            }

            TokenStore.save(newAccess, refreshToken)
            return authorized(response.request, newAccess)
        }
    }

    /** Clone [request] with Authorization: Bearer <accessToken>. */
    private fun authorized(request: Request, accessToken: String): Request =
        request.newBuilder()
            .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$accessToken")
            .build()

    private fun bearerToken(request: Request): String? {
        val header = request.header(HEADER_AUTHORIZATION) ?: return null
        return header.removePrefix(BEARER_PREFIX).trim().takeIf { it.isNotEmpty() }
    }

    private fun isRefreshRequest(request: Request): Boolean {
        val path = request.url.encodedPath
        return path.endsWith("/auth/refresh") || path.endsWith("/token/refresh")
    }

    /** Counts this response plus any prior responses in the retry chain. */
    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }

    companion object {
        // Shared lock so two parallel 401s don't both refresh at once.
        private val lock = Any()
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
