package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.core.JourneyType
import com.example.transcilmobileapp.data.model.ApiResponse
import com.example.transcilmobileapp.data.model.kyc.DigioStatusData
import com.example.transcilmobileapp.data.model.onboarding.OnboardingData
import com.example.transcilmobileapp.data.model.onboarding.OnboardingStepDto
import com.example.transcilmobileapp.data.network.FakeTranscilApi
import com.example.transcilmobileapp.repository.DigioKycRepository
import com.example.transcilmobileapp.repository.OnboardingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DigioReturnSyncTest {

    @Before
    fun setUp() {
        KycProgressRepository.clearAuthLocal()
    }

    @Test
    fun digioApproved_withoutOnboardingComplete_doesNotMarkStepsComplete() = runBlocking {
        val api = object : FakeTranscilApi() {
            override suspend fun kycSyncStatus(idempotencyKey: String) =
                ApiResponse(DigioStatusData(status = "approved"), null, null)

            override suspend fun getOnboarding() = ApiResponse(
                OnboardingData(
                    riderRole = "rider",
                    steps = listOf(
                        OnboardingStepDto(key = "personal_details", status = "complete"),
                        OnboardingStepDto(key = "aadhaar", status = "pending"),
                        OnboardingStepDto(key = "bank", status = "pending"),
                    ),
                ),
                null,
                null,
            )
        }

        DigioReturnSync.applyAfterReturn(
            digioRepository = DigioKycRepository(api),
            onboardingRepository = OnboardingRepository(api),
        )

        assertEquals(JourneyType.RENT_EV, KycProgressRepository.currentJourney())
        assertFalse(KycProgressRepository.isCompleted(KycStep.AADHAAR))
        assertFalse(KycProgressRepository.isCompleted(KycStep.BANK))
        assertTrue(KycProgressRepository.isCompleted(KycStep.PERSONAL))
    }

    @Test
    fun digioApproved_onboardingComplete_appliesGreenChecks() = runBlocking {
        val api = object : FakeTranscilApi() {
            override suspend fun kycSyncStatus(idempotencyKey: String) =
                ApiResponse(DigioStatusData(status = "approved"), null, null)

            override suspend fun getOnboarding() = ApiResponse(
                OnboardingData(
                    riderRole = "rider",
                    steps = listOf(
                        OnboardingStepDto(key = "aadhaar", status = "complete"),
                        OnboardingStepDto(key = "bank", status = "complete"),
                    ),
                ),
                null,
                null,
            )
        }

        DigioReturnSync.applyAfterReturn(
            digioRepository = DigioKycRepository(api),
            onboardingRepository = OnboardingRepository(api),
        )

        assertTrue(KycProgressRepository.isCompleted(KycStep.AADHAAR))
        assertTrue(KycProgressRepository.isCompleted(KycStep.BANK))
    }

    @Test
    fun alwaysRefetchesOnboarding_evenWhenDigioSyncFails() = runBlocking {
        val api = object : FakeTranscilApi() {
            override suspend fun kycSyncStatus(idempotencyKey: String): ApiResponse<DigioStatusData> =
                error("network")

            override suspend fun getOnboarding() = ApiResponse(
                OnboardingData(
                    riderRole = "rider",
                    steps = listOf(OnboardingStepDto(key = "address", status = "complete")),
                ),
                null,
                null,
            )
        }

        val outcome = DigioReturnSync.applyAfterReturn(
            digioRepository = DigioKycRepository(api),
            onboardingRepository = OnboardingRepository(api),
        )

        assertTrue(outcome.digioFailed)
        assertTrue(outcome.onboardingApplied)
        assertTrue(KycProgressRepository.isCompleted(KycStep.ADDRESS))
    }
}
