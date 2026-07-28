package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.core.JourneyType
import com.example.transcilmobileapp.data.model.onboarding.OnboardingData
import com.example.transcilmobileapp.data.model.onboarding.OnboardingStepDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingSyncTest {

    @Before
    fun setUp() {
        KycProgressRepository.clearAuthLocal()
    }

    @Test
    fun apply_setsJourneyAndCompletions() {
        OnboardingSync.apply(
            OnboardingData(
                riderRole = "rider",
                allComplete = false,
                steps = listOf(
                    OnboardingStepDto(
                        key = "personal_details",
                        status = "complete",
                        fields = mapOf(
                            "full_name" to "Ravi Kumar",
                            "email" to "ravi@example.com",
                            "dob" to "1990-01-01",
                            "gender" to "male",
                        ),
                    ),
                    OnboardingStepDto(key = "address", status = "pending"),
                ),
            )
        )

        assertEquals(JourneyType.RENT_EV, KycProgressRepository.currentJourney())
        assertTrue(KycProgressRepository.isCompleted(KycStep.PERSONAL))
        assertEquals("Ravi Kumar", KycProgressRepository.personalDraft().fullName)
        assertEquals("01 - 01 - 1990", KycProgressRepository.personalDraft().dateOfBirth)
    }

    @Test
    fun mapStepKey_knownKeys() {
        assertEquals(KycStep.PERSONAL, OnboardingSync.mapStepKey("personal_details"))
        assertEquals(KycStep.ADDRESS, OnboardingSync.mapStepKey("address"))
        assertEquals(KycStep.AADHAAR, OnboardingSync.mapStepKey("aadhaar"))
    }
}
