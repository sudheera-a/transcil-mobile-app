/**
 * Gson model for the `error` field inside standard API envelopes ([ApiResponse]).
 * Repositories read message/code when `data` is null and surface failures to ViewModels.
 *
 * Kotlin notes:
 * - `data class` = JSON-friendly holder with defaults for optional fields.
 * - `@SerializedName` maps snake_case JSON keys to camelCase Kotlin properties.
 */
package com.transcil.rider.data.model

import com.google.gson.annotations.SerializedName

data class ApiError(
    val code: String? = null,
    val message: String? = null,
    val retryable: Boolean? = null,
    val category: String? = null,
    @SerializedName("client_action")
    val clientAction: String? = null,
)
