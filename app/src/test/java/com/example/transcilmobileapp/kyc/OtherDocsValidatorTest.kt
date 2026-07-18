package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OtherDocsValidatorTest {

    @Test
    fun pan_valid() {
        val errors = OtherDocsValidator.validate("PAN Card", "ABCDE1234F")
        assertFalse(errors.hasErrors)
    }

    @Test
    fun pan_rejectsWrongLengthAndFormat() {
        // 9 chars — length error (screenshot-style incomplete PAN)
        assertEquals(
            R.string.error_invalid_pan_length,
            OtherDocsValidator.validate("PAN Card", "dvipa9972").documentNumber
        )
        // 10 chars but wrong pattern (digits then letters)
        assertEquals(
            R.string.error_invalid_pan,
            OtherDocsValidator.validate("PAN Card", "12345ABCDE").documentNumber
        )
        // Lowercase input is normalized and accepted when format is correct
        assertNull(OtherDocsValidator.validate("PAN Card", "abcde1234f").documentNumber)
        assertNull(OtherDocsValidator.validate("PAN Card", "dvipa9972f").documentNumber)
    }

    @Test
    fun voterId_validAndLength() {
        assertFalse(OtherDocsValidator.validate("Voter ID Card", "ABC1234567").hasErrors)
        assertEquals(
            R.string.error_invalid_voter_id_length,
            OtherDocsValidator.validate("Voter ID Card", "ABC12345").documentNumber
        )
        assertEquals(
            R.string.error_invalid_voter_id,
            OtherDocsValidator.validate("Voter ID Card", "1234567ABC").documentNumber
        )
    }

    @Test
    fun drivingLicense_validFifteenChars_stripsSeparators() {
        assertFalse(
            OtherDocsValidator.validate("Driving License", "KA0120110001234").hasErrors
        )
        assertFalse(
            OtherDocsValidator.validate("Driving License", "KA-01-2011-0001234").hasErrors
        )
        assertEquals(
            R.string.error_invalid_dl_length,
            OtherDocsValidator.validate("Driving License", "KA01201100012").documentNumber
        )
    }

    @Test
    fun blankNumber_required() {
        assertEquals(
            R.string.error_other_doc_number_required,
            OtherDocsValidator.validate("PAN Card", "   ").documentNumber
        )
    }

    @Test
    fun normalize_uppercasesAndStrips() {
        assertEquals(
            "ABCDE1234F",
            OtherDocsValidator.normalize(OtherDocumentType.PAN_CARD, "abcde1234f")
        )
        assertEquals(
            "KA0120110001234",
            OtherDocsValidator.normalize(OtherDocumentType.DRIVING_LICENSE, "ka-01 2011 0001234")
        )
    }

    @Test
    fun maxLengths() {
        assertEquals(10, OtherDocsValidator.maxLength(OtherDocumentType.PAN_CARD))
        assertEquals(10, OtherDocsValidator.maxLength(OtherDocumentType.VOTER_ID))
        assertTrue(OtherDocsValidator.maxLength(OtherDocumentType.DRIVING_LICENSE) >= 15)
    }
}
