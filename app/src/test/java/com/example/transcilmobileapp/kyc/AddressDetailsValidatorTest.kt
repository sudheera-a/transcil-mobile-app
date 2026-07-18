package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressDetailsValidatorTest {

    @Test
    fun indianStates_hasExactlyThirtySixEntries() {
        assertEquals(36, IndianStates.ALL.size)
        assertEquals(37, IndianStates.ALL_WITH_PLACEHOLDER.size)
        assertEquals(IndianStates.PLACEHOLDER, IndianStates.ALL_WITH_PLACEHOLDER.first())
    }

    @Test
    fun indianStates_containsKeyStatesAndUts() {
        assertTrue(IndianStates.isValid("Karnataka"))
        assertTrue(IndianStates.isValid("Telangana"))
        assertTrue(IndianStates.isValid("Delhi"))
        assertTrue(IndianStates.isValid("Jammu and Kashmir"))
        assertTrue(IndianStates.isValid("Ladakh"))
        assertTrue(IndianStates.isValid("Dadra and Nagar Haveli and Daman and Diu"))
        assertTrue(IndianStates.isValid("Puducherry"))
        assertFalse(IndianStates.isValid("Select state"))
        assertFalse(IndianStates.isValid("Bombay"))
    }

    @Test
    fun validInput_hasNoErrors() {
        val errors = AddressDetailsValidator.validate(
            line1 = "12 Green Villa",
            line2 = "MG Road",
            city = "Bengaluru",
            state = "Karnataka",
            pincode = "560001"
        )
        assertFalse(errors.hasErrors)
    }

    @Test
    fun line1_requiredAndMaxLength() {
        assertEquals(
            R.string.error_address_line1_required,
            AddressDetailsValidator.validate("", "", "City", "Karnataka", "560001").line1
        )
        assertEquals(
            R.string.error_address_line_too_long,
            AddressDetailsValidator.validate("A".repeat(31), "", "City", "Karnataka", "560001").line1
        )
        assertNull(
            AddressDetailsValidator.validate("A".repeat(30), "", "City", "Karnataka", "560001").line1
        )
    }

    @Test
    fun line2_optionalButMaxLength() {
        assertNull(
            AddressDetailsValidator.validate("12 Green Villa", "", "City", "Karnataka", "560001").line2
        )
        assertEquals(
            R.string.error_address_line_too_long,
            AddressDetailsValidator.validate(
                "12 Green Villa",
                "A".repeat(31),
                "City",
                "Karnataka",
                "560001"
            ).line2
        )
    }

    @Test
    fun city_and_state_and_pincode_rules() {
        assertEquals(
            R.string.error_city_required,
            AddressDetailsValidator.validate("12 Green", "", "", "Karnataka", "560001").city
        )
        assertEquals(
            R.string.error_state_required,
            AddressDetailsValidator.validate("12 Green", "", "City", "Select state", "560001").state
        )
        assertEquals(
            R.string.error_pincode_required,
            AddressDetailsValidator.validate("12 Green", "", "City", "Karnataka", "").pincode
        )
        assertEquals(
            R.string.error_invalid_pincode,
            AddressDetailsValidator.validate("12 Green", "", "City", "Karnataka", "12345").pincode
        )
        assertEquals(
            R.string.error_invalid_pincode,
            AddressDetailsValidator.validate("12 Green", "", "City", "Karnataka", "012345").pincode
        )
    }
}
