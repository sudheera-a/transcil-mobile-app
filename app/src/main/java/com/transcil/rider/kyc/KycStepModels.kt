/**
 * KYC step identifiers, status enum, UI model, and journey-specific step ordering catalog.
 * Defines which steps appear for Rent-EV vs 3PL paths and maps each step to its title string resource.
 */
package com.transcil.rider.kyc

import com.transcil.rider.R
import com.transcil.rider.core.JourneyType

/** `enum class`: canonical KYC step keys used across repository, ViewModel, and navigation. */
enum class KycStep {
    PERSONAL, AADHAAR, ADDRESS, REFERENCE, OTHER_DOCS, PAN, BANK, SELFIE
}

enum class KycStepStatus {
    COMPLETED, IN_PROGRESS, PENDING
}

/** `data class`: one row in the KYC progress accordion (step + status + display strings). */
data class KycStepUi(
    val step: KycStep,
    val status: KycStepStatus,
    val titleRes: Int,
    val subtitle: String?
)

/** `object`: journey → ordered step list and step → title string resource. */
object KycStepCatalog {
    fun stepsFor(journey: JourneyType): List<KycStep> = when (journey) {
        // Figma Rent order: Personal → Present Address → Aadhaar → Bank → Reference → Other Docs → Selfie
        JourneyType.RENT_EV -> listOf(
            KycStep.PERSONAL, KycStep.ADDRESS, KycStep.AADHAAR, KycStep.BANK,
            KycStep.REFERENCE, KycStep.OTHER_DOCS, KycStep.SELFIE
        )
        JourneyType.THREE_PL -> listOf(
            KycStep.PERSONAL, KycStep.AADHAAR, KycStep.ADDRESS,
            KycStep.REFERENCE, KycStep.OTHER_DOCS, KycStep.PAN,
            KycStep.BANK, KycStep.SELFIE
        )
    }

    fun titleRes(step: KycStep): Int = when (step) {
        KycStep.PERSONAL -> R.string.kyc_step_personal
        KycStep.AADHAAR -> R.string.kyc_step_aadhaar
        KycStep.ADDRESS -> R.string.kyc_step_address
        KycStep.REFERENCE -> R.string.kyc_step_reference
        KycStep.OTHER_DOCS -> R.string.kyc_step_other_docs
        KycStep.PAN -> R.string.kyc_step_pan
        KycStep.BANK -> R.string.kyc_step_bank
        KycStep.SELFIE -> R.string.kyc_step_selfie
    }
}
