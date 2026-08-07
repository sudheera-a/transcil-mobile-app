/**
 * Unit tests that local IFSC/bank completion survives server sync without remote bank data.
 */
package com.transcil.rider.kyc

import com.transcil.rider.core.JourneyType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Temporary bank gate: valid IFSC/format marks BANK complete without Digio.
 */
class BankLocalIfscCompleteTest {

    @Before
    fun setUp() {
        KycProgressRepository.clearAuthLocal()
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        KycProgressRepository.markCompleted(KycStep.PERSONAL)
        KycProgressRepository.markCompleted(KycStep.ADDRESS)
        KycProgressRepository.markCompleted(KycStep.AADHAAR)
    }

    @Test
    fun validIfsc_formatPasses() {
        val errors = BankDetailsValidator.validate(
            holderName = "Ravi Kumar",
            accountNumber = "1234567890",
            confirmAccountNumber = "1234567890",
            ifsc = "HDFC0001234",
            consent = true,
        )
        assertFalse(errors.hasErrors)
    }

    @Test
    fun invalidIfsc_fails() {
        val errors = BankDetailsValidator.validate(
            holderName = "Ravi Kumar",
            accountNumber = "1234567890",
            confirmAccountNumber = "1234567890",
            ifsc = "BAD",
            consent = true,
        )
        assertTrue(errors.hasErrors)
        assertTrue(errors.ifsc != null)
    }

    @Test
    fun markCompletedLocalOnly_survivesServerSyncWithoutBank() {
        assertFalse(KycProgressRepository.isCompleted(KycStep.BANK))
        KycProgressRepository.markCompletedLocalOnly(KycStep.BANK)
        assertTrue(KycProgressRepository.isCompleted(KycStep.BANK))

        KycProgressRepository.syncStepStatuses(
            completedSteps = mapOf(KycStep.PERSONAL to "Completed", KycStep.ADDRESS to "Completed"),
            inProgressSteps = emptySet(),
        )
        assertTrue(KycProgressRepository.isCompleted(KycStep.BANK))
        assertTrue(KycProgressRepository.isCompleted(KycStep.PERSONAL))
    }
}
