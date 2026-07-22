package com.example.transcilmobileapp.data.repository

import com.example.transcilmobileapp.data.model.PostDto
import com.example.transcilmobileapp.data.network.ApiClient

class DemoRepository {

    suspend fun loadPost(id: Int): PostDto {
        return ApiClient.demoApi.getPost(id)
    }
}