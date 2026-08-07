/**
 * Handles post-Digio deep-link return: syncs Digio session status, refreshes onboarding, picks toast.
 * Called from DigioKycCallbackActivity when the rider lands back in the app after e-KYC in the browser.
 */
package com.transcil.rider.kyc

import com.transcil.rider.repository.DigioKycRepository
import com.transcil.rider.repository.OnboardingRepository

/** `object`: coordinates two suspend repository calls in sequence. */
object DigioReturnSync {

    /** `data class`: bundles sync outcome for toast/navigation decisions. */
    data class Outcome(
        val digioStatus: String?,
        val digioFailed: Boolean,
        val onboardingApplied: Boolean,
    )

    /** `suspend fun`: must be called from a coroutine (e.g. lifecycleScope.launch in Activity). */
    suspend fun applyAfterReturn(
        digioRepository: DigioKycRepository = DigioKycRepository(),
        onboardingRepository: OnboardingRepository = OnboardingRepository(),
    ): Outcome {
        val digioResult = digioRepository.syncStatus()
        val onboardingResult = onboardingRepository.getOnboarding()
        onboardingResult.onSuccess { OnboardingSync.apply(it) }
        return Outcome(
            digioStatus = digioResult.getOrNull()?.status,
            digioFailed = digioResult.isFailure,
            onboardingApplied = onboardingResult.isSuccess,
        )
    }

    fun toastFor(outcome: Outcome): ToastKind {
        if (outcome.digioFailed) return ToastKind.SYNC_FAILED
        return when (outcome.digioStatus?.lowercase()) {
            "approved" -> ToastKind.NONE
            else -> ToastKind.PENDING
        }
    }

    enum class ToastKind { NONE, PENDING, SYNC_FAILED }
}
