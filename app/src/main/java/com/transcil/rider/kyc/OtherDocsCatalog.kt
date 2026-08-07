/**
 * Maps onboarding API `options.doc_types` codes to human-readable spinner labels for "Other Docs".
 * Keeps UI labels stable when the server returns snake_case codes like `voter_id` or `driving_license`.
 */
package com.transcil.rider.kyc

/** `object`: stateless lookup table shared by KycProgress accordion and OnboardingSync. */
object OtherDocsCatalog {

    val DEFAULT_LABELS = listOf("Voter ID Card", "Driving License", "PAN Card")

    fun labelForApi(code: String): String? = when (code.trim().lowercase()) {
        "voter_id", "voterid" -> "Voter ID Card"
        "driving_license", "driving_licence", "dl" -> "Driving License"
        "pan", "pan_card" -> "PAN Card"
        else -> null
    }

    fun uiLabels(apiCodes: List<String>): List<String> {
        val mapped = apiCodes.mapNotNull { labelForApi(it) }.distinct()
        return mapped.ifEmpty { DEFAULT_LABELS }
    }
}
