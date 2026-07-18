package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.R

enum class OtherDocumentType {
    VOTER_ID,
    DRIVING_LICENSE,
    PAN_CARD;

    companion object {
        fun fromLabel(label: String): OtherDocumentType? = when (label.trim()) {
            "Voter ID Card" -> VOTER_ID
            "Driving License" -> DRIVING_LICENSE
            "PAN Card" -> PAN_CARD
            else -> null
        }
    }
}

data class OtherDocsFieldErrors(
    val documentType: Int? = null,
    val documentNumber: Int? = null
) {
    val hasErrors: Boolean get() = documentType != null || documentNumber != null
}

object OtherDocsValidator {

    /** PAN: 5 letters + 4 digits + 1 letter (e.g. ABCDE1234F). */
    private val panRegex = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")

    /** EPIC / Voter ID: 3 letters + 7 digits (e.g. ABC1234567). */
    private val voterIdRegex = Regex("^[A-Z]{3}[0-9]{7}$")

    /**
     * Indian DL (common MoRTH format without separators):
     * 2-letter state + 2-digit RTO + 4-digit year + 7-digit unique = 15 chars
     * (e.g. KA0120110001234).
     */
    private val drivingLicenseRegex = Regex("^[A-Z]{2}[0-9]{2}[0-9]{4}[0-9]{7}$")

    fun maxLength(type: OtherDocumentType): Int = when (type) {
        OtherDocumentType.PAN_CARD -> 10
        OtherDocumentType.VOTER_ID -> 10
        OtherDocumentType.DRIVING_LICENSE -> 18 // allows typed spaces/hyphens before normalize
    }

    fun helperRes(type: OtherDocumentType): Int = when (type) {
        OtherDocumentType.PAN_CARD -> R.string.kyc_helper_pan
        OtherDocumentType.VOTER_ID -> R.string.kyc_helper_voter_id
        OtherDocumentType.DRIVING_LICENSE -> R.string.kyc_helper_driving_license
    }

    fun hintRes(type: OtherDocumentType): Int = when (type) {
        OtherDocumentType.PAN_CARD -> R.string.kyc_hint_pan
        OtherDocumentType.VOTER_ID -> R.string.kyc_hint_voter_id
        OtherDocumentType.DRIVING_LICENSE -> R.string.kyc_hint_driving_license
    }

    fun normalize(type: OtherDocumentType, raw: String): String {
        val upper = raw.trim().uppercase()
        return when (type) {
            OtherDocumentType.DRIVING_LICENSE ->
                upper.filter { it.isLetterOrDigit() }
            OtherDocumentType.PAN_CARD,
            OtherDocumentType.VOTER_ID ->
                upper.filter { it.isLetterOrDigit() }
        }
    }

    fun validate(documentTypeLabel: String, documentNumber: String): OtherDocsFieldErrors {
        val type = OtherDocumentType.fromLabel(documentTypeLabel)
            ?: return OtherDocsFieldErrors(documentType = R.string.error_other_doc_type_required)

        val normalized = normalize(type, documentNumber)
        if (normalized.isEmpty()) {
            return OtherDocsFieldErrors(documentNumber = R.string.error_other_doc_number_required)
        }

        val numberError = when (type) {
            OtherDocumentType.PAN_CARD -> validatePan(normalized)
            OtherDocumentType.VOTER_ID -> validateVoterId(normalized)
            OtherDocumentType.DRIVING_LICENSE -> validateDrivingLicense(normalized)
        }
        return OtherDocsFieldErrors(documentNumber = numberError)
    }

    private fun validatePan(value: String): Int? {
        if (value.length != 10) return R.string.error_invalid_pan_length
        if (!panRegex.matches(value)) return R.string.error_invalid_pan
        return null
    }

    private fun validateVoterId(value: String): Int? {
        if (value.length != 10) return R.string.error_invalid_voter_id_length
        if (!voterIdRegex.matches(value)) return R.string.error_invalid_voter_id
        return null
    }

    private fun validateDrivingLicense(value: String): Int? {
        if (value.length != 15) return R.string.error_invalid_dl_length
        if (!drivingLicenseRegex.matches(value)) return R.string.error_invalid_dl
        return null
    }
}
