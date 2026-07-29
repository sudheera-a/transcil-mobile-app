package com.example.transcilmobileapp.data.model.kyc

import com.google.gson.annotations.SerializedName

data class KycUploadRequest(
    @SerializedName("doc_type") val docType: String,
    @SerializedName("content_type") val contentType: String,
    @SerializedName("size_bytes") val sizeBytes: Long,
    val sha256: String? = null,
)

data class KycUploadData(
    @SerializedName("kyc_id") val kycId: String,
    @SerializedName("media_id") val mediaId: String? = null,
    @SerializedName("upload_url") val uploadUrl: String,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("required_headers") val requiredHeaders: Map<String, String> = emptyMap(),
    @SerializedName("max_size_bytes") val maxSizeBytes: Long? = null,
)

data class KycSubmitRequest(
    @SerializedName("kyc_id") val kycId: String,
    @SerializedName("doc_number") val docNumber: String,
    @SerializedName("holder_name") val holderName: String,
    @SerializedName("issued_on") val issuedOn: String? = null,
    @SerializedName("expires_on") val expiresOn: String? = null,
)

data class KycDocumentSummary(
    @SerializedName("kyc_id") val kycId: String? = null,
    @SerializedName("doc_type") val docType: String? = null,
    @SerializedName("doc_number_masked") val docNumberMasked: String? = null,
    @SerializedName("holder_name") val holderName: String? = null,
    val status: String? = null,
    @SerializedName("media_id") val mediaId: String? = null,
    @SerializedName("submitted_at") val submittedAt: String? = null,
    @SerializedName("rejection_reason") val rejectionReason: String? = null,
)

data class KycListData(
    val documents: List<KycDocumentSummary> = emptyList(),
)

data class ReferenceUpsertRequest(
    val relation: String,
    @SerializedName("mobile_e164") val mobileE164: String,
)

data class ReferenceData(
    @SerializedName("rider_id") val riderId: String? = null,
    val relation: String? = null,
    @SerializedName("mobile_e164") val mobileE164: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
)

data class PanVerifyRequest(
    @SerializedName("pan_number") val panNumber: String,
    val name: String,
    val dob: String? = null,
)

data class PanVerifyData(
    val verified: Boolean = false,
    @SerializedName("verification_id") val verificationId: String? = null,
    @SerializedName("name_match") val nameMatch: Boolean = true,
    @SerializedName("dob_match") val dobMatch: Boolean = true,
    @SerializedName("registered_name") val registeredName: String? = null,
    val status: String? = null,
    @SerializedName("verified_at") val verifiedAt: String? = null,
)
