/**
 * Unit tests for [KycDocumentRepository.uploadAndSubmit]: headers, S3 upload, and submit ordering.
 */
package com.transcil.rider.repository

import com.transcil.rider.data.model.ApiResponse
import com.transcil.rider.data.model.kyc.KycDocumentSummary
import com.transcil.rider.data.model.kyc.KycSubmitRequest
import com.transcil.rider.data.model.kyc.KycUploadData
import com.transcil.rider.data.model.kyc.KycUploadRequest
import com.transcil.rider.data.network.FakeTranscilApi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KycDocumentRepositoryTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun uploadAndSubmit_putsRequiredHeadersThenSubmits() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        val uploadUrl = server.url("/s3/put").toString()
        var submitBody: KycSubmitRequest? = null
        val api = object : FakeTranscilApi() {
            override suspend fun kycUploadRequest(
                idempotencyKey: String,
                body: KycUploadRequest,
            ): ApiResponse<KycUploadData> = ApiResponse(
                KycUploadData(
                    kycId = "01JKYCTEST0000000000000000",
                    uploadUrl = uploadUrl,
                    requiredHeaders = mapOf(
                        "Content-Type" to "image/jpeg",
                        "x-amz-meta-test" to "1",
                    ),
                ),
                null,
                null,
            )

            override suspend fun kycSubmit(
                idempotencyKey: String,
                body: KycSubmitRequest,
            ): ApiResponse<KycDocumentSummary> {
                submitBody = body
                return ApiResponse(
                    KycDocumentSummary(kycId = body.kycId, status = "submitted"),
                    null,
                    null,
                )
            }
        }

        val bytes = byteArrayOf(1, 2, 3, 4)
        val result = KycDocumentRepository(api, OkHttpClient()).uploadAndSubmit(
            docType = "selfie",
            contentType = "image/jpeg",
            bytes = bytes,
            docNumber = "SELFIE",
            holderName = "Ravi Kumar",
        )

        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("image/jpeg", recorded.getHeader("Content-Type"))
        assertEquals("1", recorded.getHeader("x-amz-meta-test"))
        assertEquals("01JKYCTEST0000000000000000", submitBody?.kycId)
        assertEquals("SELFIE", submitBody?.docNumber)
    }

    @Test
    fun uploadAndSubmit_s3Failure_doesNotSubmit() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403))
        val uploadUrl = server.url("/s3/put").toString()
        var submitCalled = false
        val api = object : FakeTranscilApi() {
            override suspend fun kycUploadRequest(
                idempotencyKey: String,
                body: KycUploadRequest,
            ): ApiResponse<KycUploadData> = ApiResponse(
                KycUploadData(
                    kycId = "01JKYCTEST0000000000000000",
                    uploadUrl = uploadUrl,
                    requiredHeaders = mapOf("Content-Type" to "image/jpeg"),
                ),
                null,
                null,
            )

            override suspend fun kycSubmit(
                idempotencyKey: String,
                body: KycSubmitRequest,
            ): ApiResponse<KycDocumentSummary> {
                submitCalled = true
                return unusedSubmit()
            }
        }

        val result = KycDocumentRepository(api, OkHttpClient()).uploadAndSubmit(
            docType = "voter_id",
            contentType = "image/jpeg",
            bytes = byteArrayOf(9),
            docNumber = "ABC1234567",
            holderName = "Ravi",
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().startsWith("S3_UPLOAD_FAILED"))
        assertTrue(!submitCalled)
    }

    private fun unusedSubmit(): ApiResponse<KycDocumentSummary> = error("unused")
}
