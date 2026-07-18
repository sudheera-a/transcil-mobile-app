package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.core.JourneyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KycProgressRepositoryTest {

    @Before
    fun setUp() {
        KycProgressRepository.reset()
    }

    @Test
    fun rentEv_hasSevenSteps_includingBank_withoutPan() {
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        val steps = KycProgressRepository.stepsFor(JourneyType.RENT_EV)
        assertEquals(7, steps.size)
        assertTrue(steps.contains(KycStep.BANK))
        assertFalse(steps.contains(KycStep.PAN))
        assertEquals(
            listOf(
                KycStep.PERSONAL, KycStep.ADDRESS, KycStep.AADHAAR, KycStep.BANK,
                KycStep.REFERENCE, KycStep.OTHER_DOCS, KycStep.SELFIE
            ),
            steps
        )
    }

    @Test
    fun threePl_hasEightSteps_includingBankAndPan() {
        KycProgressRepository.startJourney(JourneyType.THREE_PL)
        val steps = KycProgressRepository.stepsFor(JourneyType.THREE_PL)
        assertEquals(8, steps.size)
        assertTrue(steps.contains(KycStep.PAN))
        assertTrue(steps.contains(KycStep.BANK))
    }

    @Test
    fun afterPersonal_addressIsInProgress_andOnlyAllowedOpen() {
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        KycProgressRepository.markCompleted(KycStep.PERSONAL)
        assertEquals(KycStep.ADDRESS, KycProgressRepository.inProgressStep())
        assertTrue(KycProgressRepository.canOpen(KycStep.PERSONAL))
        assertTrue(KycProgressRepository.canOpen(KycStep.ADDRESS))
        assertFalse(KycProgressRepository.canOpen(KycStep.AADHAAR))
        assertEquals(1, KycProgressRepository.completedCount())
        assertEquals(14, KycProgressRepository.progressPercent()) // round(100/7)
    }

    @Test
    fun rentEv_markCompletedPan_isIgnored() {
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        KycProgressRepository.markCompleted(KycStep.PAN)
        assertEquals(0, KycProgressRepository.completedCount())
        assertEquals(KycStep.PERSONAL, KycProgressRepository.inProgressStep())
    }

    @Test
    fun addressDraft_persistsAcrossReads() {
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        KycProgressRepository.saveAddress(
            AddressDraft(
                line1 = "12 Green Villa",
                line2 = "MG Road",
                city = "Bengaluru",
                state = "Karnataka",
                pincode = "560001"
            )
        )
        val draft = KycProgressRepository.addressDraft()
        assertEquals("12 Green Villa", draft.line1)
        assertEquals("560001", draft.pincode)
    }

    @Test
    fun aadhaarDraft_otpSent_persistsUntilReset() {
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        KycProgressRepository.saveAadhaar(
            AadhaarDraft(
                aadhaarNumber = "123456789012",
                consent = true,
                otpSent = true,
                otp = "123456"
            )
        )
        val draft = KycProgressRepository.aadhaarDraft()
        assertEquals("123456789012", draft.aadhaarNumber)
        assertTrue(draft.consent)
        assertTrue(draft.otpSent)
        assertEquals("123456", draft.otp)
    }

    @Test
    fun startJourney_sameJourney_keepsDrafts() {
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        KycProgressRepository.saveAddress(AddressDraft(line1 = "Keep me"))
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        assertEquals("Keep me", KycProgressRepository.addressDraft().line1)
    }

    @Test
    fun startJourney_switchJourney_clearsDrafts() {
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        KycProgressRepository.saveAddress(AddressDraft(line1 = "Wipe me"))
        KycProgressRepository.startJourney(JourneyType.THREE_PL)
        assertEquals("", KycProgressRepository.addressDraft().line1)
    }

    @Test
    fun sessionMobile_savedNormalized_survivesJourneyReset() {
        KycProgressRepository.saveSessionMobile("+91 98765-43210")
        assertEquals("9876543210", KycProgressRepository.sessionMobile())
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        KycProgressRepository.startJourney(JourneyType.THREE_PL)
        assertEquals("9876543210", KycProgressRepository.sessionMobile())
    }
}
