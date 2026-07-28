package com.example.transcilmobileapp.data.model

import com.google.gson.annotations.SerializedName

data class ApiError(
    val code: String? = null,
    val message: String? = null,
    val retryable: Boolean? = null,
    val category: String? = null,
    @SerializedName("client_action")
    val clientAction: String? = null,
)
