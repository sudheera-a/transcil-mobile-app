/**
 * Unit tests for [TokenAuthenticator]: 401 refresh, retry, token clearing, and concurrency.
 */
package com.transcil.rider.data.network

import com.transcil.rider.data.local.TokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
            authenticatorCalled.get(),
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

        assertFalse(
            "TokenStore should be cleared after 401 with no refresh token",
            TokenStore.hasToken(),
        )
    }

    @Test
    fun tokenAuthenticator_refreshesAndRetries() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(
            MockResponse().setBody(
                """{"data":{"access_token":"new-access","id_token":"id","token_type":"Bearer","expires_in":3600},"meta":null,"error":null}""",
            ).addHeader("Content-Type", "application/json"),
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        TokenStore.save("old-access", "refresh-1")
        AuthTokenRefresher.baseUrlOverride = server.url("/").toString()

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .authenticator(TokenAuthenticator())
            .build()

        val response = client.newCall(
            Request.Builder().url(server.url("/secure")).build(),
        ).execute()

        assertEquals(200, response.code)
        assertEquals("new-access", TokenStore.getAccessToken())
        assertEquals("refresh-1", TokenStore.getRefreshToken())
        // protected 401 + refresh + retry
        assertEquals(3, server.requestCount)
        assertTrue(server.takeRequest().path!!.endsWith("/secure"))
        assertTrue(server.takeRequest().path!!.endsWith("/v1/auth/refresh"))
        assertTrue(server.takeRequest().path!!.endsWith("/secure"))
    }

    @Test
    fun tokenAuthenticator_reusesTokenRefreshedByAnotherThread() {
        // First request fails; refresh succeeds; retry succeeds.
        // Second concurrent request also 401s but should reuse new-access (no second refresh).
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(
            MockResponse().setBody(
                """{"data":{"access_token":"new-access","id_token":"id","token_type":"Bearer","expires_in":3600},"meta":null,"error":null}""",
            ).addHeader("Content-Type", "application/json"),
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok-1"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok-2"))

        TokenStore.save("old-access", "refresh-1")
        AuthTokenRefresher.baseUrlOverride = server.url("/").toString()

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .authenticator(TokenAuthenticator())
            .build()

        val start = CountDownLatch(1)
        val done = CountDownLatch(2)
        val successes = AtomicInteger(0)
        val pool = Executors.newFixedThreadPool(2)
        repeat(2) {
            pool.execute {
                start.await(2, TimeUnit.SECONDS)
                try {
                    client.newCall(Request.Builder().url(server.url("/secure")).build())
                        .execute()
                        .use { if (it.isSuccessful) successes.incrementAndGet() }
                } finally {
                    done.countDown()
                }
            }
        }
        start.countDown()
        assertTrue(done.await(5, TimeUnit.SECONDS))
        pool.shutdown()

        assertEquals(2, successes.get())
        assertEquals("new-access", TokenStore.getAccessToken())
        // 2 protected 401s + 1 refresh + 2 retries = 5 (not 6 with a second refresh)
        assertEquals(5, server.requestCount)
    }

    @Test
    fun tokenAuthenticator_refreshEndpoint401_clearsTokensWithoutLoop() {
        server.enqueue(MockResponse().setResponseCode(401))
        TokenStore.save("access", "refresh-1")
        AuthTokenRefresher.baseUrlOverride = server.url("/").toString()

        val client = OkHttpClient.Builder()
            .authenticator(TokenAuthenticator())
            .build()

        client.newCall(
            Request.Builder()
                .url(server.url("/v1/auth/refresh"))
                .post(ByteArray(0).toRequestBody(null))
                .build(),
        ).execute().close()

        assertFalse(TokenStore.hasToken())
        assertEquals(1, server.requestCount)
    }
}
