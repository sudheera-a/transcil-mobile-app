package com.example.transcilmobileapp.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val data: T?,
    val meta: ApiMeta?,
    val error: ApiError?,
)

data class ApiMeta(
    @SerializedName(value = "requestId", alternate = ["request_id"])
    val requestId: String?,
    val nextCursor: String? = null,
)
