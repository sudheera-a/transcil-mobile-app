package com.example.transcilmobileapp.data.network

import com.example.transcilmobileapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val okHttp: OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor(AuthInterceptor())
        if (BuildConfig.DEBUG) {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
        }
        authenticator(TokenAuthenticator())
    }.build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val demoApi: DemoApi = retrofit.create(DemoApi::class.java)
    val transcilApi: TranscilApi = retrofit.create(TranscilApi::class.java)
}
