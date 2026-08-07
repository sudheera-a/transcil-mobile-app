/**
 * Unit tests for [OnboardingSync]: applying API payloads, subtitles, and step key mapping.
 */
package com.transcil.rider.kyc

import com.transcil.rider.core.JourneyType
import com.transcil.rider.data.model.onboarding.OnboardingData
import com.transcil.rider.data.model.onboarding.OnboardingStepDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun formatCompletedSubtitle_formatsIsoUtc() {
        val formatted = OnboardingSync.formatCompletedSubtitle("2026-07-28T06:53:31Z")
        assertTrue(formatted.startsWith("Completed "))
        assertFalse(formatted.contains("T06:53:31Z"))
        assertTrue(formatted.contains("28 Jul 2026"))
    }

    @Test
    fun formatCompletedSubtitle_blankFallsBack() {
        assertEquals("Completed", OnboardingSync.formatCompletedSubtitle(null))
        assertEquals("Completed", OnboardingSync.formatCompletedSubtitle("  "))
        assertEquals("Completed", OnboardingSync.formatCompletedSubtitle("not-a-date"))
    }

    @Test
    fun formatCompletedNow_matchesDisplayStyle() {
        val formatted = OnboardingSync.formatCompletedNow()
        assertTrue(formatted.startsWith("Completed "))
        assertFalse(formatted.contains("just now"))
    }

    @Test
    fun apply_formatsCompletedAtSubtitle() {
        OnboardingSync.apply(
            OnboardingData(
                riderRole = "rider",
                steps = listOf(
                    OnboardingStepDto(
                        key = "personal_details",
                        status = "complete",
                        completedAt = "2026-07-28T06:53:31Z",
                    ),
                ),
            ),
        )
        val subtitle = KycProgressRepository.uiSteps()
            .first { it.step == KycStep.PERSONAL }
            .subtitle
        assertTrue(subtitle!!.startsWith("Completed "))
        assertFalse(subtitle.contains("T06:53:31Z"))
    }

    @Test
    fun relationLabelForUi_mapsApiToSpinner() {
        assertEquals("Mother", OnboardingSync.relationLabelForUi("mother"))
        assertEquals("Father", OnboardingSync.relationLabelForUi("FATHER"))
        assertEquals("7780118557", OnboardingSync.mobile10FromE164("+917780118557"))
    }

    @Test
    fun apply_hydratesReferenceFields() {
        OnboardingSync.apply(
            OnboardingData(
                riderRole = "rider",
                steps = listOf(
                    OnboardingStepDto(
                        key = "reference",
                        status = "complete",
                        completedAt = "2026-07-28T06:53:31Z",
                        fields = mapOf(
                            "relation" to "mother",
                            "mobile_e164" to "+917780118557",
                        ),
                    ),
                ),
            ),
        )
        assertTrue(KycProgressRepository.isCompleted(KycStep.REFERENCE))
        assertEquals("Mother", KycProgressRepository.referenceDraft().relation)
        assertEquals("7780118557", KycProgressRepository.referenceDraft().mobile)
    }

    @Test
    fun mapStepKey_knownKeys() {
        assertEquals(KycStep.PERSONAL, OnboardingSync.mapStepKey("personal_details"))
        assertEquals(KycStep.ADDRESS, OnboardingSync.mapStepKey("address"))
        assertEquals(KycStep.AADHAAR, OnboardingSync.mapStepKey("aadhaar"))
        assertEquals(KycStep.BANK, OnboardingSync.mapStepKey("bank"))
        assertEquals(KycStep.REFERENCE, OnboardingSync.mapStepKey("reference"))
        assertEquals(KycStep.OTHER_DOCS, OnboardingSync.mapStepKey("other_docs"))
        assertEquals(KycStep.PAN, OnboardingSync.mapStepKey("pan"))
        assertEquals(KycStep.SELFIE, OnboardingSync.mapStepKey("selfie"))
    }

    @Test
    fun apply_inProgress_doesNotMarkComplete() {
        OnboardingSync.apply(
            OnboardingData(
                riderRole = "rider",
                steps = listOf(
                    OnboardingStepDto(key = "aadhaar", status = "in_progress"),
                    OnboardingStepDto(key = "bank", status = "in_progress"),
                ),
            ),
        )

        assertFalse(KycProgressRepository.isCompleted(KycStep.AADHAAR))
        assertFalse(KycProgressRepository.isCompleted(KycStep.BANK))
        assertTrue(KycProgressRepository.isServerInProgress(KycStep.AADHAAR))
        assertTrue(KycProgressRepository.isServerInProgress(KycStep.BANK))
    }
}
