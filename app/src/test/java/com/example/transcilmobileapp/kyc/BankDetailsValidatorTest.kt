package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankDetailsValidatorTest {

    @Test
    fun validInput_hasNoErrors() {
        val errors = BankDetailsValidator.validate(
            holderName = "Sai Kumar",
            accountNumber = "123456789012",
            confirmAccountNumber = "123456789012",
            ifsc = "HDFC0001234",
            consent = true,
        )
        assertFalse(errors.hasErrors)
        assertNull(errors.holderName)
        assertNull(errors.accountNumber)
        assertNull(errors.confirmAccountNumber)
        assertNull(errors.ifsc)
        assertNull(errors.consent)
    }

    @Test
    fun accountNumber_tooShort_invalid() {
        val errors = validBase(accountNumber = "12345678", confirmAccountNumber = "12345678")
        assertEquals(R.string.error_account_number_invalid, errors.accountNumber)
    }

    @Test
    fun accountNumber_tooLong_invalid() {
        val errors = validBase(
            accountNumber = "1".repeat(19),
            confirmAccountNumber = "1".repeat(19),
        )
        assertEquals(R.string.error_account_number_invalid, errors.accountNumber)
    }

    @Test
    fun confirmAccount_mismatch() {
        val errors = validBase(confirmAccountNumber = "999999999999")
        assertEquals(R.string.error_account_mismatch, errors.confirmAccountNumber)
    }

    @Test
    fun ifsc_invalidFormat() {
        val errors = validBase(ifsc = "HDFC1234567")
        assertEquals(R.string.error_invalid_ifsc, errors.ifsc)
    }

    @Test
    fun ifsc_normalizesLowercase() {
        val errors = validBase(ifsc = "hdfc0001234")
        assertNull(errors.ifsc)
        assertFalse(errors.hasErrors)
    }

    @Test
    fun consent_required() {
        val errors = validBase(consent = false)
        assertEquals(R.string.error_bank_consent, errors.consent)
        assertTrue(errors.hasErrors)
    }

    private fun validBase(
        holderName: String = "Sai Kumar",
        accountNumber: String = "123456789012",
        confirmAccountNumber: String = "123456789012",
        ifsc: String = "HDFC0001234",
        consent: Boolean = true,
    ): BankFieldErrors = BankDetailsValidator.validate(
        holderName = holderName,
        accountNumber = accountNumber,
        confirmAccountNumber = confirmAccountNumber,
        ifsc = ifsc,
        consent = consent,
    )
}
