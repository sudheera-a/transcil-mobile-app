/**
 * Unit tests for [ApiResponse] JSON envelope parsing and typed error deserialization.
 */
package com.transcil.rider.data.model

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiResponseTest {

    @Test
    fun parsesEnvelopeWithNullError() {
        val json = """
            {
              "data": { "foo": "bar" },
              "meta": { "requestId": "req-1", "nextCursor": null },
              "error": null
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponse<JsonObject>>() {}.type
        val res: ApiResponse<JsonObject> = Gson().fromJson(json, type)

        assertEquals("bar", res.data?.get("foo")?.asString)
        assertEquals("req-1", res.meta?.requestId)
        assertNull(res.meta?.nextCursor)
        assertNull(res.error)
    }

    @Test
    fun parsesIdentityEnvelopeWithSnakeCaseMetaAndError() {
        val json = """
            {
              "data": { "session": "abc" },
              "meta": { "request_id": "01J", "service": "identity", "timestamp": "2026-05-25T13:30:00.000Z" },
              "error": null
            }
        """.trimIndent()
        val type = object : TypeToken<ApiResponse<JsonObject>>() {}.type
        val res: ApiResponse<JsonObject> = Gson().fromJson(json, type)
        assertEquals("01J", res.meta?.requestId)
        assertNull(res.error)
    }

    @Test
    fun parsesTypedErrorObject() {
        val json = """
            {
              "data": null,
              "meta": { "request_id": "01J" },
              "error": {
                "code": "AUTH_OTP_INVALID",
                "message": "Incorrect OTP",
                "retryable": false,
                "category": "auth",
                "client_action": "stay_on_screen"
              }
            }
        """.trimIndent()
        val type = object : TypeToken<ApiResponse<Any?>>() {}.type
        val res: ApiResponse<Any?> = Gson().fromJson(json, type)
        assertEquals("AUTH_OTP_INVALID", res.error?.code)
        assertEquals("stay_on_screen", res.error?.clientAction)
    }
}
