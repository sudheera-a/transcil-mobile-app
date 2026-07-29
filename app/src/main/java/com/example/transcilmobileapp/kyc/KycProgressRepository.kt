package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.core.Gender
import com.example.transcilmobileapp.core.JourneyType
import com.example.transcilmobileapp.data.local.KycLocalStore
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

data class BankDraft(
    val holderName: String = "",
    val accountNumber: String = "",
    val confirmAccountNumber: String = "",
    val ifsc: String = "",
    val consent: Boolean = false
)

/**
 * In-memory KYC progress + form drafts for the current session.
 * Drafts persist across accordion re-renders and Activity back/forth until [startJourney]/[reset].
 */
object KycProgressRepository {

    private var journey: JourneyType? = null
    private val completed = mutableMapOf<KycStep, String>()
    /** Server-owned `in_progress` steps from GET /me/onboarding (waiting Digio/review). */
    private val serverInProgress = mutableSetOf<KycStep>()
    /**
     * Steps completed only on-device (no server API yet). Survives [syncStepStatuses].
     * ponytail: used for BANK until a bank-verify API exists.
     */
    private val localOnlyCompleted = mutableMapOf<KycStep, String>()
    private var personalDraft = PersonalDraft()
    private var addressDraft = AddressDraft()
    private var aadhaarDraft = AadhaarDraft()
    private var referenceDraft = ReferenceDraft()
    private var otherDocsDraft = OtherDocsDraft()
    /** API `options.doc_types` codes from onboarding (e.g. pan, voter_id). */
    private var otherDocTypeCodes: List<String> = emptyList()
    private var bankDraft = BankDraft()
    private var sessionMobile: String = ""

    fun reset() {
        journey = null
        completed.clear()
        serverInProgress.clear()
        localOnlyCompleted.clear()
        personalDraft = PersonalDraft()
        addressDraft = AddressDraft()
        aadhaarDraft = AadhaarDraft()
        referenceDraft = ReferenceDraft()
        otherDocsDraft = OtherDocsDraft()
        otherDocTypeCodes = emptyList()
        bankDraft = BankDraft()
        // Keep sessionMobile across journey reset — it belongs to auth, not KYC drafts.
    }

    /** Full local wipe on logout (drafts + auth-linked mobile). */
    fun clearAuthLocal() {
        reset()
        sessionMobile = ""
        KycLocalStore.clear()
    }

    /** Reload bank draft + local-only completion after journey is known (process death). */
    fun restorePersistedBank() {
        val loaded = KycLocalStore.loadBank() ?: return
        val (draft, subtitle) = loaded
        bankDraft = draft
        if (!subtitle.isNullOrBlank()) {
            localOnlyCompleted[KycStep.BANK] = subtitle
            if (KycStep.BANK in orderedSteps()) {
                completed[KycStep.BANK] = subtitle
            }
        }
    }

    fun startJourney(journey: JourneyType) {
        // Keep progress/drafts when continuing the same journey; wipe when switching.
        if (this.journey != null && this.journey != journey) {
            reset()
        }
        this.journey = journey
    }

    fun currentJourney(): JourneyType? = journey

    fun saveSessionMobile(mobile: String) {
        sessionMobile = mobile.filter { it.isDigit() }.takeLast(10)
    }

    fun sessionMobile(): String = sessionMobile

    fun stepsFor(journey: JourneyType): List<KycStep> = KycStepCatalog.stepsFor(journey)

    fun markCompleted(step: KycStep, completedSubtitle: String = "Completed just now") {
        if (step !in orderedSteps()) return
        completed[step] = completedSubtitle
    }

    /** Local-only completion that [syncStepStatuses] will not wipe. */
    fun markCompletedLocalOnly(step: KycStep, completedSubtitle: String = "Completed just now") {
        if (step !in orderedSteps()) return
        localOnlyCompleted[step] = completedSubtitle
        completed[step] = completedSubtitle
        if (step == KycStep.BANK) {
            KycLocalStore.saveBank(bankDraft, completedSubtitle)
        }
    }

    /** Replace catalog step completion from GET /me/onboarding (server truth). */
    fun syncStepStatuses(
        completedSteps: Map<KycStep, String>,
        inProgressSteps: Set<KycStep> = emptySet(),
    ) {
        val catalog = orderedSteps().toSet()
        completed.keys.filter { it in catalog }.forEach { completed.remove(it) }
        completedSteps.forEach { (step, subtitle) ->
            if (step in catalog) {
                completed[step] = subtitle
                // Server caught up — drop local-only override for this step.
                if (localOnlyCompleted.remove(step) != null && step == KycStep.BANK) {
                    KycLocalStore.saveBank(bankDraft, null)
                }
            }
        }
        localOnlyCompleted.forEach { (step, subtitle) ->
            if (step in catalog) completed[step] = subtitle
        }
        serverInProgress.clear()
        inProgressSteps.filter { it in catalog && it !in completed }.forEach { serverInProgress += it }
    }

    fun isCompleted(step: KycStep): Boolean = step in completed

    fun isServerInProgress(step: KycStep): Boolean = step in serverInProgress

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

    fun saveOtherDocTypeOptions(apiCodes: List<String>) {
        otherDocTypeCodes = apiCodes.map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** Spinner labels from last onboarding sync (falls back to defaults). */
    fun otherDocTypeLabels(): List<String> = OtherDocsCatalog.uiLabels(otherDocTypeCodes)

    fun saveBank(draft: BankDraft) {
        bankDraft = draft
        val subtitle = localOnlyCompleted[KycStep.BANK] ?: completed[KycStep.BANK]
        KycLocalStore.saveBank(draft, subtitle)
    }

    fun bankDraft(): BankDraft = bankDraft

    fun uiSteps(): List<KycStepUi> {
        val localCurrent = inProgressStep()
        return orderedSteps().map { step ->
            val status = when {
                step in completed -> KycStepStatus.COMPLETED
                step in serverInProgress -> KycStepStatus.IN_PROGRESS
                step == localCurrent -> KycStepStatus.IN_PROGRESS
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

    fun inProgressStep(): KycStep? =
        orderedSteps().firstOrNull { it !in completed && it !in serverInProgress }
            ?: orderedSteps().firstOrNull { it in serverInProgress }

    fun canOpen(step: KycStep): Boolean {
        val inProgress = inProgressStep()
        return step in completed || step == inProgress || step in serverInProgress
    }

    private fun orderedSteps(): List<KycStep> {
        val current = journey ?: return emptyList()
        return KycStepCatalog.stepsFor(current)
    }
}
