package com.example.transcilmobileapp.data.network

import com.example.transcilmobileapp.data.local.TokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        TokenStore.clear()
        AuthTokenRefresher.baseUrlOverride = null
    }

    @After
    fun tearDown() {
        server.shutdown()
        TokenStore.clear()
        AuthTokenRefresher.baseUrlOverride = null
    }

    @Test
    fun okHttp_callsAuthenticator_automatically_on401() {
        server.enqueue(MockResponse().setResponseCode(401))

        val authenticatorCalled = AtomicBoolean(false)
        val client = OkHttpClient.Builder()
            .authenticator { _, response ->
                authenticatorCalled.set(true)
                assertTrue(response.code == 401)
                null // do not retry
            }
            .build()

        val request = Request.Builder()
            .url(server.url("/secure"))
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.code == 401)
        }

        assertTrue(
            "Authenticator must be invoked automatically on HTTP 401",
            authenticatorCalled.get()
        )
    }

    @Test
    fun tokenAuthenticator_runs_on401_andClearsToken_whenNoRefreshToken() {
        server.enqueue(MockResponse().setResponseCode(401))
        TokenStore.save("invalid-access-token")

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .authenticator(TokenAuthenticator())
            .build()

        val request = Request.Builder()
            .url(server.url("/secure"))
            .build()

        client.newCall(request).execute().close()

        // TokenAuthenticator clears tokens when it cannot refresh
        assertFalse(
            "TokenStore should be cleared after 401 with no refresh token",
            TokenStore.hasToken()
        )
    }

    @Test
    fun tokenAuthenticator_refreshesAndRetries() {
        // 1) protected call → 401
        server.enqueue(MockResponse().setResponseCode(401))
        // 2) refresh → 200 with new access
        server.enqueue(
            MockResponse().setBody(
                """{"data":{"access_token":"new-access","id_token":"id","token_type":"Bearer","expires_in":3600},"meta":null,"error":null}"""
            ).addHeader("Content-Type", "application/json")
        )
        // 3) retry → 200
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        TokenStore.save("old-access", "refresh-1")

        // Point refresher base URL at mock server — inject via AuthTokenRefresher.baseUrlOverride for tests
        AuthTokenRefresher.baseUrlOverride = server.url("/").toString()

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .authenticator(TokenAuthenticator())
            .build()

        val response = client.newCall(
            Request.Builder().url(server.url("/secure")).build()
        ).execute()

        assertEquals(200, response.code)
        assertEquals("new-access", TokenStore.getAccessToken())
        assertEquals("refresh-1", TokenStore.getRefreshToken())
    }
}
