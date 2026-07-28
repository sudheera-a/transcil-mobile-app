package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.data.model.onboarding.OnboardingData
import com.example.transcilmobileapp.data.model.onboarding.OnboardingStepDto
import com.example.transcilmobileapp.repository.fromApiDob
import com.example.transcilmobileapp.repository.parseApiGender
import com.example.transcilmobileapp.repository.riderRoleToJourney
import java.util.Locale

/**
 * Applies GET /v1/me/onboarding into local KYC drafts/progress.
 * Local catalog order stays; server owns completion truth.
 */
object OnboardingSync {

    fun apply(data: OnboardingData) {
        riderRoleToJourney(data.riderRole)?.let {
            KycProgressRepository.startJourney(it)
        }
        val completed = linkedMapOf<KycStep, String>()
        data.steps.forEach { step ->
            val kyc = mapStepKey(step.key) ?: return@forEach
            hydrate(kyc, step)
            if (step.status.equals("complete", ignoreCase = true)) {
                completed[kyc] = step.completedAt?.let { "Completed $it" } ?: "Completed"
            }
        }
        if (KycProgressRepository.currentJourney() != null) {
            KycProgressRepository.syncStepStatuses(completed)
        }
    }

    fun mapStepKey(key: String): KycStep? = when (key.lowercase(Locale.US)) {
        "personal_details" -> KycStep.PERSONAL
        "address" -> KycStep.ADDRESS
        "aadhaar" -> KycStep.AADHAAR
        "bank" -> KycStep.BANK
        "reference" -> KycStep.REFERENCE
        "other_docs" -> KycStep.OTHER_DOCS
        "pan" -> KycStep.PAN
        "selfie" -> KycStep.SELFIE
        else -> null
    }

    private fun hydrate(step: KycStep, dto: OnboardingStepDto) {
        val fields = dto.fields ?: return
        when (step) {
            KycStep.PERSONAL -> {
                val draft = KycProgressRepository.personalDraft()
                KycProgressRepository.savePersonal(
                    draft.copy(
                        fullName = fields["full_name"] ?: draft.fullName,
                        email = fields["email"] ?: draft.email,
                        dateOfBirth = fields["dob"]?.let { fromApiDob(it) } ?: draft.dateOfBirth,
                        gender = parseApiGender(fields["gender"]) ?: draft.gender,
                    )
                )
            }
            KycStep.ADDRESS -> {
                val draft = KycProgressRepository.addressDraft()
                KycProgressRepository.saveAddress(
                    draft.copy(
                        line1 = fields["address_line1"] ?: draft.line1,
                        line2 = fields["address_line2"] ?: draft.line2,
                        city = fields["city"] ?: draft.city,
                        state = fields["state"] ?: draft.state,
                        pincode = fields["pincode"] ?: draft.pincode,
                    )
                )
            }
            else -> Unit
        }
    }
}
