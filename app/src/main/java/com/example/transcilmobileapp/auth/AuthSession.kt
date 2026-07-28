package com.example.transcilmobileapp.auth

import android.content.Context
import android.content.Intent
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.data.local.TokenStore
import com.example.transcilmobileapp.data.model.onboarding.OnboardingData
import com.example.transcilmobileapp.home.HomeDashboardActivity
import com.example.transcilmobileapp.journey.ChooseJourneyActivity
import com.example.transcilmobileapp.kyc.KycProgressActivity
import com.example.transcilmobileapp.kyc.KycProgressRepository
import com.example.transcilmobileapp.kyc.OnboardingSync
import com.example.transcilmobileapp.onboarding.OnboardingActivity
import com.example.transcilmobileapp.repository.AuthRepository
import com.example.transcilmobileapp.repository.OnboardingRepository

/** Pure cold-start destination (testable without Android Context). */
sealed class ColdStartTarget {
    data object Onboarding : ColdStartTarget()
    data object ChooseJourney : ColdStartTarget()
    data object KycProgress : ColdStartTarget()
    data class Home(val status: KycStatus) : ColdStartTarget()
}

/**
 * Phase A/B session glue: logout + cold-start restore from GET /me/onboarding.
 */
object AuthSession {

    fun shouldRestoreSession(): Boolean = TokenStore.hasToken()

    fun resolveColdStartTarget(
        hasToken: Boolean,
        onboarding: OnboardingData?,
    ): ColdStartTarget {
        if (!hasToken) return ColdStartTarget.Onboarding
        if (onboarding == null) return ColdStartTarget.Home(KycStatus.PENDING)
        return when {
            onboarding.riderRole.isNullOrBlank() -> ColdStartTarget.ChooseJourney
            !onboarding.allComplete -> ColdStartTarget.KycProgress
            else -> ColdStartTarget.Home(
                if (onboarding.documents?.verified == true) {
                    KycStatus.APPROVED
                } else {
                    KycStatus.PENDING
                },
            )
        }
    }

    suspend fun resolveColdStart(
        context: Context,
        onboardingRepository: OnboardingRepository = OnboardingRepository(),
    ): Intent {
        if (!shouldRestoreSession()) {
            return intentFor(context, ColdStartTarget.Onboarding)
        }
        val onboarding = onboardingRepository.getOnboarding().getOrNull()
        if (onboarding != null) {
            OnboardingSync.apply(onboarding)
        }
        return intentFor(context, resolveColdStartTarget(hasToken = true, onboarding = onboarding))
    }

    suspend fun signOut(authRepository: AuthRepository = AuthRepository()) {
        runCatching { authRepository.logout().getOrThrow() }
        TokenStore.clear()
        KycProgressRepository.clearAuthLocal()
    }

    fun openSignedOut(context: Context) {
        context.startActivity(
            Intent(context, WelcomeActivity::class.java).clearTask(),
        )
    }

    private fun intentFor(context: Context, target: ColdStartTarget): Intent = when (target) {
        ColdStartTarget.Onboarding -> Intent(context, OnboardingActivity::class.java).clearTask()
        ColdStartTarget.ChooseJourney -> Intent(context, ChooseJourneyActivity::class.java).clearTask()
        ColdStartTarget.KycProgress -> Intent(context, KycProgressActivity::class.java).clearTask()
        is ColdStartTarget.Home -> HomeDashboardActivity.createIntent(context, target.status).clearTask()
    }

    private fun Intent.clearTask(): Intent = apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
}
