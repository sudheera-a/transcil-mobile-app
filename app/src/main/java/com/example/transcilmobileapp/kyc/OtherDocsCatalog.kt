package com.example.transcilmobileapp.kyc

/** Maps onboarding `options.doc_types` API codes ↔ spinner labels. */
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
