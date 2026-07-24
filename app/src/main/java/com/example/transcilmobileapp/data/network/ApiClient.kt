package com.example.transcilmobileapp.data.network

import com.example.transcilmobileapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = OkHttpClient.Builder()
        // 1) Add Bearer token (if saved)
        .addInterceptor(AuthInterceptor())
        // 2) Log request/response (you'll see Authorization here)
        .addInterceptor(logging)
        // 3) On 401 -> try recover / clear tokens
        .authenticator(TokenAuthenticator())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val demoApi: DemoApi = retrofit.create(DemoApi::class.java)
    val transcilApi: TranscilApi = retrofit.create(TranscilApi::class.java)
}