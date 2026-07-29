package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.repository.DigioKycRepository
import com.example.transcilmobileapp.repository.OnboardingRepository

/**
 * Digio deep-link return: sync Digio, then always refresh onboarding (completion authority).
 */
object DigioReturnSync {

    data class Outcome(
        val digioStatus: String?,
        val digioFailed: Boolean,
        val onboardingApplied: Boolean,
    )

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
