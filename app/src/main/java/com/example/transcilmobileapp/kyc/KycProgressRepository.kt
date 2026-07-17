package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.core.Gender
import com.example.transcilmobileapp.core.JourneyType
import kotlin.math.round

data class PersonalDraft(
    val fullName: String = "",
    val email: String = "",
    val dateOfBirth: String = "",
    val gender: Gender? = null
)

data class AddressDraft(
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = ""
)

data class AadhaarDraft(
    val aadhaarNumber: String = "",
    val consent: Boolean = false,
    val otpSent: Boolean = false,
    val otp: String = ""
)

data class ReferenceDraft(
    val relation: String = "",
    val mobile: String = ""
)

data class OtherDocsDraft(
    val documentType: String = "",
    val documentNumber: String = ""
)

/**
 * In-memory KYC progress + form drafts for the current session.
 * Drafts persist across accordion re-renders and Activity back/forth until [startJourney]/[reset].
 */
object KycProgressRepository {

    private var journey: JourneyType? = null
    private val completed = mutableMapOf<KycStep, String>()
    private var personalDraft = PersonalDraft()
    private var addressDraft = AddressDraft()
    private var aadhaarDraft = AadhaarDraft()
    private var referenceDraft = ReferenceDraft()
    private var otherDocsDraft = OtherDocsDraft()

    fun reset() {
        journey = null
        completed.clear()
        personalDraft = PersonalDraft()
        addressDraft = AddressDraft()
        aadhaarDraft = AadhaarDraft()
        referenceDraft = ReferenceDraft()
        otherDocsDraft = OtherDocsDraft()
    }

    fun startJourney(journey: JourneyType) {
        // Keep progress/drafts when continuing the same journey; wipe when switching.
        if (this.journey != null && this.journey != journey) {
            reset()
        }
        this.journey = journey
    }

    fun currentJourney(): JourneyType? = journey

    fun stepsFor(journey: JourneyType): List<KycStep> = KycStepCatalog.stepsFor(journey)

    fun markCompleted(step: KycStep, completedSubtitle: String = "Completed just now") {
        if (step !in orderedSteps()) return
        completed[step] = completedSubtitle
    }

    fun isCompleted(step: KycStep): Boolean = step in completed

    fun savePersonal(draft: PersonalDraft) {
        personalDraft = draft
    }

    fun personalDraft(): PersonalDraft = personalDraft

    fun saveAddress(draft: AddressDraft) {
        addressDraft = draft
    }

    fun addressDraft(): AddressDraft = addressDraft

    fun saveAadhaar(draft: AadhaarDraft) {
        aadhaarDraft = draft
    }

    fun aadhaarDraft(): AadhaarDraft = aadhaarDraft

    fun saveReference(relation: String, mobile: String) {
        referenceDraft = ReferenceDraft(relation = relation.trim(), mobile = mobile.trim())
    }

    fun referenceDraft(): ReferenceDraft = referenceDraft

    fun saveOtherDocs(documentType: String, documentNumber: String) {
        otherDocsDraft = OtherDocsDraft(
            documentType = documentType.trim(),
            documentNumber = documentNumber.trim()
        )
    }

    fun otherDocsDraft(): OtherDocsDraft = otherDocsDraft

    fun uiSteps(): List<KycStepUi> {
        val inProgress = inProgressStep()
        return orderedSteps().map { step ->
            val status = when {
                step in completed -> KycStepStatus.COMPLETED
                step == inProgress -> KycStepStatus.IN_PROGRESS
                else -> KycStepStatus.PENDING
            }
            KycStepUi(
                step = step,
                status = status,
                titleRes = KycStepCatalog.titleRes(step),
                subtitle = if (status == KycStepStatus.COMPLETED) completed[step] else null
            )
        }
    }

    fun completedCount(): Int = orderedSteps().count { it in completed }

    fun totalCount(): Int = orderedSteps().size

    fun progressPercent(): Int {
        val total = totalCount()
        if (total == 0) return 0
        return round(100f * completedCount() / total).toInt()
    }

    fun inProgressStep(): KycStep? = orderedSteps().firstOrNull { it !in completed }

    fun canOpen(step: KycStep): Boolean {
        val inProgress = inProgressStep()
        return step in completed || step == inProgress
    }

    private fun orderedSteps(): List<KycStep> {
        val current = journey ?: return emptyList()
        return KycStepCatalog.stepsFor(current)
    }
}
