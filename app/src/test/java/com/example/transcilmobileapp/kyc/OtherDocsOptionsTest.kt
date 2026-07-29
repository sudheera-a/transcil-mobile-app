package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.core.JourneyType
import com.example.transcilmobileapp.data.model.onboarding.OnboardingData
import com.example.transcilmobileapp.data.model.onboarding.OnboardingStepDto
import com.example.transcilmobileapp.data.model.onboarding.OnboardingStepOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OtherDocsOptionsTest {

    @Before
    fun setUp() {
        KycProgressRepository.clearAuthLocal()
    }

    @Test
    fun labelForApiCode_mapsKnownTypes() {
        assertEquals("PAN Card", OtherDocsCatalog.labelForApi("pan"))
        assertEquals("Voter ID Card", OtherDocsCatalog.labelForApi("voter_id"))
        assertEquals("Driving License", OtherDocsCatalog.labelForApi("driving_license"))
    }

    @Test
    fun uiLabels_fromApiCodes_preservesOrder() {
        assertEquals(
            listOf("Voter ID Card", "PAN Card", "Driving License"),
            OtherDocsCatalog.uiLabels(listOf("voter_id", "pan", "driving_license")),
        )
    }

    @Test
    fun uiLabels_empty_fallsBackToDefaults() {
        assertEquals(OtherDocsCatalog.DEFAULT_LABELS, OtherDocsCatalog.uiLabels(emptyList()))
    }

    @Test
    fun onboardingSync_storesDocTypesFromOptions() {
        OnboardingSync.apply(
            OnboardingData(
                riderRole = "rider",
                steps = listOf(
                    OnboardingStepDto(
                        key = "other_docs",
                        status = "pending",
                        options = OnboardingStepOptions(
                            docTypes = listOf("pan", "voter_id"),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(JourneyType.RENT_EV, KycProgressRepository.currentJourney())
        assertEquals(
            listOf("PAN Card", "Voter ID Card"),
            KycProgressRepository.otherDocTypeLabels(),
        )
    }

    @Test
    fun inProgress_withoutLocalComplete_formStaysEditable() {
        assertTrue(
            KycProgressViewModel.isOtherDocsFormEditable(
                status = KycStepStatus.IN_PROGRESS,
                hasSubmittedDraft = false,
                inlineEditing = false,
            ),
        )
    }
}
