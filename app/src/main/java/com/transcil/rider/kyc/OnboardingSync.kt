/**
 * Applies GET /v1/me/onboarding response into local KYC drafts and step completion state.
 * Server owns completion truth; this object hydrates drafts and maps API step keys to [KycStep].
 */
package com.transcil.rider.kyc

import com.transcil.rider.data.model.onboarding.OnboardingData
import com.transcil.rider.data.model.onboarding.OnboardingStepDto
import com.transcil.rider.repository.fromApiDob
import com.transcil.rider.repository.parseApiGender
import com.transcil.rider.repository.riderRoleToJourney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** `object`: called after every onboarding fetch to keep UI in sync with server. */
object OnboardingSync {

    private val isoParsers = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
    )

    /** Formats API `completed_at` for KYC step subtitles. */
    fun formatCompletedSubtitle(completedAt: String?): String {
        if (completedAt.isNullOrBlank()) return "Completed"
        val date = parseIsoInstant(completedAt.trim()) ?: return "Completed"
        return formatCompletedAt(date)
    }

    /** Local completion subtitle using device clock (same style as API timestamps). */
    fun formatCompletedNow(now: Date = Date()): String = formatCompletedAt(now)

    private fun formatCompletedAt(date: Date): String {
        val out = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return "Completed ${out.format(date)}"
    }

    fun parseIsoInstant(raw: String): java.util.Date? {
        for (pattern in isoParsers) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }.parse(raw)
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    fun apply(data: OnboardingData) {
        riderRoleToJourney(data.riderRole)?.let {
            KycProgressRepository.startJourney(it)
        }
        // Restore local bank before sync so local-only completion survives onboarding refresh.
        KycProgressRepository.restorePersistedBank()
        val completed = linkedMapOf<KycStep, String>()
        val inProgress = linkedSetOf<KycStep>()
        data.steps.forEach { step ->
            val kyc = mapStepKey(step.key) ?: return@forEach
            hydrate(kyc, step)
            when {
                step.status.equals("complete", ignoreCase = true) -> {
                    completed[kyc] = formatCompletedSubtitle(step.completedAt)
                }
                step.status.equals("in_progress", ignoreCase = true) -> {
                    inProgress += kyc
                }
            }
        }
        if (KycProgressRepository.currentJourney() != null) {
            KycProgressRepository.syncStepStatuses(completed, inProgress)
        }
    }

    /** Maps API relation (`mother`) to spinner label (`Mother`). */
    fun relationLabelForUi(apiRelation: String?): String {
        if (apiRelation.isNullOrBlank()) return ""
        val known = listOf("Mother", "Father", "Brother", "Sister", "Wife", "Husband")
        return known.firstOrNull { it.equals(apiRelation.trim(), ignoreCase = true) }
            ?: apiRelation.trim().replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
            }
    }

    fun mobile10FromE164(mobileE164: String?): String =
        mobileE164?.filter { it.isDigit() }?.takeLast(10).orEmpty()

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

    /** `private fun`: copies API step fields into the matching KycProgressRepository draft. */
    private fun hydrate(step: KycStep, dto: OnboardingStepDto) {
        if (step == KycStep.OTHER_DOCS) {
            dto.options?.docTypes?.takeIf { it.isNotEmpty() }?.let {
                KycProgressRepository.saveOtherDocTypeOptions(it)
            }
        }
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
            KycStep.REFERENCE -> {
                val relation = relationLabelForUi(fields["relation"])
                val mobile = mobile10FromE164(
                    fields["mobile_e164"] ?: fields["mobile"],
                )
                if (relation.isNotBlank() || mobile.isNotBlank()) {
                    val draft = KycProgressRepository.referenceDraft()
                    KycProgressRepository.saveReference(
                        relation = relation.ifBlank { draft.relation },
                        mobile = mobile.ifBlank { draft.mobile },
                    )
                }
            }
            KycStep.OTHER_DOCS -> {
                val apiType = fields["doc_type"] ?: fields["document_type"]
                val label = apiType?.let { OtherDocsCatalog.labelForApi(it) }
                    ?: fields["document_label"]
                val number = fields["doc_number"] ?: fields["document_number"]
                if (!label.isNullOrBlank() || !number.isNullOrBlank()) {
                    val draft = KycProgressRepository.otherDocsDraft()
                    KycProgressRepository.saveOtherDocs(
                        documentType = label?.takeIf { it.isNotBlank() } ?: draft.documentType,
                        documentNumber = number?.takeIf { it.isNotBlank() } ?: draft.documentNumber,
                    )
                }
            }
            else -> Unit
        }
    }

    fun applyReference(relation: String?, mobileE164: String?) {
        val label = relationLabelForUi(relation)
        val mobile = mobile10FromE164(mobileE164)
        if (label.isBlank() && mobile.isBlank()) return
        val draft = KycProgressRepository.referenceDraft()
        KycProgressRepository.saveReference(
            relation = label.ifBlank { draft.relation },
            mobile = mobile.ifBlank { draft.mobile },
        )
    }
}
