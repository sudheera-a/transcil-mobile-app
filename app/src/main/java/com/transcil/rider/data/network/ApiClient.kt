/**
 * App-wide HTTP client: builds Retrofit + OkHttp once, exposes [transcilApi].
 *
 * Stack:
 * - OkHttp = low-level HTTP (interceptors, auth retry).
 * - Retrofit = turns [TranscilApi] interface methods into real network calls.
 * - GsonConverterFactory = JSON ↔ Kotlin data classes.
 *
 * `object` = singleton so every repository shares one client.
 * `private val` = created once; not visible outside this file.
 */
package com.transcil.rider.data.network

import com.transcil.rider.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val okHttp: OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor(AuthInterceptor()) // attach Bearer token on each request
        if (BuildConfig.DEBUG) {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY // log request/response in debug
                },
            )
        }
        authenticator(TokenAuthenticator()) // on 401, refresh token and retry
    }.build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val transcilApi: TranscilApi = retrofit.create(TranscilApi::class.java)
}
