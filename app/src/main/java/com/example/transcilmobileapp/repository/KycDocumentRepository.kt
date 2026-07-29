package com.example.transcilmobileapp.repository

import com.example.transcilmobileapp.data.model.kyc.KycDocumentSummary
import com.example.transcilmobileapp.data.model.kyc.KycListData
import com.example.transcilmobileapp.data.model.kyc.KycSubmitRequest
import com.example.transcilmobileapp.data.model.kyc.KycUploadRequest
import com.example.transcilmobileapp.data.network.TranscilApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.util.UUID

class KycDocumentRepository(
    private val api: TranscilApi = com.example.transcilmobileapp.data.network.ApiClient.transcilApi,
    private val uploadClient: OkHttpClient = OkHttpClient(),
) {
    suspend fun uploadAndSubmit(
        docType: String,
        contentType: String,
        bytes: ByteArray,
        docNumber: String,
        holderName: String,
    ): Result<KycDocumentSummary> = runCatching {
        require(bytes.isNotEmpty()) { "EMPTY_FILE" }
        val sha = sha256Hex(bytes)
        val uploadKey = UUID.randomUUID().toString()
        val uploadRes = api.kycUploadRequest(
            uploadKey,
            KycUploadRequest(
                docType = docType,
                contentType = contentType,
                sizeBytes = bytes.size.toLong(),
                sha256 = sha,
            ),
        )
        uploadRes.error?.let { error(it.message ?: it.code ?: "KYC_UPLOAD_REQUEST_FAILED") }
        val upload = uploadRes.data ?: error("KYC_UPLOAD_EMPTY")

        withContext(Dispatchers.IO) {
            putBinary(upload.uploadUrl, bytes, contentType, upload.requiredHeaders)
        }

        val submitRes = api.kycSubmit(
            UUID.randomUUID().toString(),
            KycSubmitRequest(
                kycId = upload.kycId,
                docNumber = docNumber,
                holderName = holderName.trim(),
            ),
        )
        submitRes.error?.let {
            if (it.code == "CONFLICT_KYC_DOC_PENDING") error("CONFLICT_KYC_DOC_PENDING")
            error(it.message ?: it.code ?: "KYC_SUBMIT_FAILED")
        }
        submitRes.data ?: error("KYC_SUBMIT_EMPTY")
    }

    suspend fun listDocuments(): Result<KycListData> = runCatching {
        val res = api.listKyc()
        res.error?.let { error(it.message ?: it.code ?: "KYC_LIST_FAILED") }
        res.data ?: KycListData()
    }

    fun latestRejectionReason(docs: KycListData, docType: String): String? =
        docs.documents
            .filter { it.docType.equals(docType, ignoreCase = true) }
            .firstOrNull { it.status.equals("rejected", ignoreCase = true) }
            ?.rejectionReason

    private fun putBinary(
        uploadUrl: String,
        bytes: ByteArray,
        contentType: String,
        requiredHeaders: Map<String, String>,
    ) {
        val mediaType = contentType.toMediaTypeOrNull()
        val body = bytes.toRequestBody(mediaType)
        val builder = Request.Builder().url(uploadUrl).put(body)
        requiredHeaders.forEach { (k, v) -> builder.header(k, v) }
        if (!requiredHeaders.keys.any { it.equals("Content-Type", ignoreCase = true) }) {
            builder.header("Content-Type", contentType)
        }
        uploadClient.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                error("S3_UPLOAD_FAILED:${response.code}")
            }
        }
    }

    companion object {
        fun sha256Hex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }

        /** Single API `doc_type` from onboarding options → one S3 object. */
        fun apiDocType(label: String): String? = when (label.trim()) {
            "Voter ID Card" -> "voter_id"
            "Driving License" -> "driving_license"
            "PAN Card" -> "pan"
            else -> null
        }
    }
}
