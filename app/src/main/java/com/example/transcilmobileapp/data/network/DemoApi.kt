package com.example.transcilmobileapp.data.network

import com.example.transcilmobileapp.data.model.PostDto
import retrofit2.http.GET
import retrofit2.http.Path

interface DemoApi {

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): PostDto
}