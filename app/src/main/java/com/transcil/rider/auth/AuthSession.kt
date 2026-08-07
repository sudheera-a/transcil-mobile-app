/**
 * Session helpers: decide where to go on cold start, and sign the user out.
 * Used from splash ([com.transcil.rider.splash.MainActivity]) and logout paths.
 *
 * Kotlin notes:
 * - `sealed class` = closed set of subtypes (compiler knows every possible case in `when`).
 *   Here: Onboarding | ChooseJourney | KycProgress | Home — no other destinations allowed.
 * - `object` = singleton with functions (no need to `new` AuthSession).
 * - `suspend fun` = coroutine function; can wait for network without blocking the UI thread.
 *   Call only from another suspend fun or from `lifecycleScope.launch { ... }`.
 * - `private fun` = helper visible only inside this object.
 */
package com.transcil.rider.auth

import android.content.Context
import android.content.Intent
import com.transcil.rider.core.KycStatus
import com.transcil.rider.data.local.TokenStore
import com.transcil.rider.data.model.onboarding.OnboardingData
import com.transcil.rider.home.HomeDashboardActivity
import com.transcil.rider.journey.ChooseJourneyActivity
import com.transcil.rider.kyc.KycProgressActivity
import com.transcil.rider.kyc.KycProgressRepository
import com.transcil.rider.kyc.OnboardingSync
import com.transcil.rider.onboarding.OnboardingActivity
import com.transcil.rider.repository.AuthRepository
import com.transcil.rider.repository.OnboardingRepository

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
            onboarding.documents?.verified == true -> ColdStartTarget.Home(KycStatus.APPROVED)
            !onboarding.allComplete -> ColdStartTarget.KycProgress
            onboarding.documents?.overall.equals("in_progress", ignoreCase = true) ->
                ColdStartTarget.Home(KycStatus.PENDING)
            else -> ColdStartTarget.Home(KycStatus.PENDING)
        }
    }

    /** Fetches onboarding if logged in, then returns an Intent to the right screen. */
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

    /** Best-effort server logout, then clear local tokens / KYC cache. */
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

    /** Clears the back stack so the user cannot press Back into a logged-in screen. */
    private fun Intent.clearTask(): Intent = apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
}
