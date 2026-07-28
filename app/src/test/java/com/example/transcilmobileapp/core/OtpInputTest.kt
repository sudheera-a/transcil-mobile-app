package com.example.transcilmobileapp.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpInputTest {
    @Test
    fun extractSixDigitCode_fromTypicalSms() {
        assertEquals(
            "482913",
            OtpInput.extractSixDigitCode("Your Transcil OTP is 482913. Valid for 5 mins."),
        )
    }

    @Test
    fun extractSixDigitCode_ignoresLongerDigitRuns() {
        assertNull(OtpInput.extractSixDigitCode("Ref 1234567 code missing"))
        assertEquals("123456", OtpInput.extractSixDigitCode("code 123456 phone 9876543210"))
    }

    @Test
    fun previousIndexOnEmptyDelete_stepsLeftOnlyWhenEmpty() {
        assertEquals(3, OtpInput.previousIndexOnEmptyDelete(focusedIndex = 4, isEmpty = true))
        assertNull(OtpInput.previousIndexOnEmptyDelete(focusedIndex = 4, isEmpty = false))
        assertNull(OtpInput.previousIndexOnEmptyDelete(focusedIndex = 0, isEmpty = true))
    }
}
