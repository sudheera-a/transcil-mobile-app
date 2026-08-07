/**
 * Generic wrapper for Transcil REST responses: payload in `data`, tracing in `meta`, failures in `error`.
 * Retrofit/Gson deserializes most endpoints into [ApiResponse] of a specific `T` (see auth/KYC DTOs).
 *
 * Kotlin notes:
 * - Generic [ApiResponse]<T> = one envelope type reused for every endpoint's data shape.
 * - `@SerializedName` with `alternate` accepts either camelCase or snake_case from the server.
 */
package com.transcil.rider.data.model

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
