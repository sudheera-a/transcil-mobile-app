package com.example.transcilmobileapp.auth

import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.data.local.TokenStore
import com.example.transcilmobileapp.data.model.ApiResponse
import com.example.transcilmobileapp.data.model.auth.AuthLogoutData
import com.example.transcilmobileapp.data.model.onboarding.OnboardingData
import com.example.transcilmobileapp.data.model.onboarding.OnboardingDocumentsStatus
import com.example.transcilmobileapp.data.network.FakeTranscilApi
import com.example.transcilmobileapp.kyc.KycProgressRepository
import com.example.transcilmobileapp.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthSessionTest {

    @Before
    fun setUp() {
        TokenStore.clear()
        KycProgressRepository.clearAuthLocal()
    }

    @Test
    fun shouldRestoreSession_followsAccessToken() {
        assertFalse(AuthSession.shouldRestoreSession())
        TokenStore.save("access", "refresh")
        assertTrue(AuthSession.shouldRestoreSession())
        TokenStore.clear()
        assertFalse(AuthSession.shouldRestoreSession())
    }

    @Test
    fun signOut_clearsLocalEvenWhenApiFails() = runBlocking {
        TokenStore.save("access", "refresh")
        KycProgressRepository.saveSessionMobile("9876543210")
        val repo = AuthRepository(object : FakeTranscilApi() {
            override suspend fun authLogout() = error("network down")
        })

        AuthSession.signOut(repo)

        assertFalse(TokenStore.hasToken())
        assertFalse(AuthSession.shouldRestoreSession())
        assertTrue(KycProgressRepository.sessionMobile().isEmpty())
    }

    @Test
    fun signOut_clearsLocalWhenApiSucceeds() = runBlocking {
        TokenStore.save("access", "refresh")
        val repo = AuthRepository(object : FakeTranscilApi() {
            override suspend fun authLogout(): ApiResponse<AuthLogoutData> =
                ApiResponse(AuthLogoutData(ok = true), null, null)
        })

        AuthSession.signOut(repo)

        assertFalse(TokenStore.hasToken())
    }

    @Test
    fun coldStart_noToken_goesToOnboarding() {
        assertEquals(
            ColdStartTarget.Onboarding,
            AuthSession.resolveColdStartTarget(hasToken = false, onboarding = null),
        )
    }

    @Test
    fun coldStart_tokenButOnboardingUnavailable_goesHomePending() {
        assertEquals(
            ColdStartTarget.Home(KycStatus.PENDING),
            AuthSession.resolveColdStartTarget(hasToken = true, onboarding = null),
        )
    }

    @Test
    fun coldStart_noRiderRole_goesToChooseJourney() {
        assertEquals(
            ColdStartTarget.ChooseJourney,
            AuthSession.resolveColdStartTarget(
                hasToken = true,
                onboarding = OnboardingData(riderRole = null, allComplete = false),
            ),
        )
        assertEquals(
            ColdStartTarget.ChooseJourney,
            AuthSession.resolveColdStartTarget(
                hasToken = true,
                onboarding = OnboardingData(riderRole = "  ", allComplete = false),
            ),
        )
    }

    @Test
    fun coldStart_incompleteOnboarding_goesToKycProgress() {
        assertEquals(
            ColdStartTarget.KycProgress,
            AuthSession.resolveColdStartTarget(
                hasToken = true,
                onboarding = OnboardingData(riderRole = "rent_ev", allComplete = false),
            ),
        )
    }

    @Test
    fun coldStart_completeVerified_goesHomeApproved() {
        assertEquals(
            ColdStartTarget.Home(KycStatus.APPROVED),
            AuthSession.resolveColdStartTarget(
                hasToken = true,
                onboarding = OnboardingData(
                    riderRole = "rent_ev",
                    allComplete = true,
                    documents = OnboardingDocumentsStatus(verified = true),
                ),
            ),
        )
    }

    @Test
    fun coldStart_completeUnverified_goesHomePending() {
        assertEquals(
            ColdStartTarget.Home(KycStatus.PENDING),
            AuthSession.resolveColdStartTarget(
                hasToken = true,
                onboarding = OnboardingData(
                    riderRole = "rent_ev",
                    allComplete = true,
                    documents = OnboardingDocumentsStatus(verified = false),
                ),
            ),
        )
    }
}
