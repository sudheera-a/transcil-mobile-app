package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.core.JourneyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * After Other Docs upload succeeds, step must leave the empty re-upload form
 * even if onboarding only reports in_progress (or lags).
 */
class OtherDocsLocalCompleteTest {

    @Before
    fun setUp() {
        KycProgressRepository.clearAuthLocal()
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        KycProgressRepository.markCompleted(KycStep.PERSONAL)
        KycProgressRepository.markCompleted(KycStep.ADDRESS)
        KycProgressRepository.markCompleted(KycStep.AADHAAR)
        KycProgressRepository.markCompleted(KycStep.BANK)
        KycProgressRepository.markCompleted(KycStep.REFERENCE)
    }

    @Test
    fun markCompletedLocalOnly_survivesServerSyncWithoutOtherDocsComplete() {
        KycProgressRepository.markCompletedLocalOnly(KycStep.OTHER_DOCS)
        assertTrue(KycProgressRepository.isCompleted(KycStep.OTHER_DOCS))

        KycProgressRepository.syncStepStatuses(
            completedSteps = mapOf(
                KycStep.PERSONAL to "Completed",
                KycStep.ADDRESS to "Completed",
                KycStep.AADHAAR to "Completed",
                KycStep.BANK to "Completed",
                KycStep.REFERENCE to "Completed",
            ),
            inProgressSteps = setOf(KycStep.OTHER_DOCS),
        )
        assertTrue(KycProgressRepository.isCompleted(KycStep.OTHER_DOCS))
        assertFalse(KycProgressRepository.isServerInProgress(KycStep.OTHER_DOCS))
        assertEquals(KycStep.SELFIE, KycProgressRepository.inProgressStep())
    }
}
