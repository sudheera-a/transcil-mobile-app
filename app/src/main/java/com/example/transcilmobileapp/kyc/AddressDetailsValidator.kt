package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.R

data class AddressFieldErrors(
    val line1: Int? = null,
    val line2: Int? = null,
    val city: Int? = null,
    val state: Int? = null,
    val pincode: Int? = null
) {
    val hasErrors: Boolean
        get() = line1 != null || line2 != null || city != null || state != null || pincode != null
}

object AddressDetailsValidator {

    private const val MAX_ADDRESS_LINE = 30
    private const val MIN_ADDRESS_LINE1 = 3
    private const val MIN_CITY = 2
    private const val MAX_CITY = 40
    private const val PINCODE_LENGTH = 6

    // Letters, digits, spaces, and common address punctuation.
    private val addressLineRegex = Regex("^[A-Za-z0-9][A-Za-z0-9 ,./#'\\-]*$")
    private val cityRegex = Regex("^[A-Za-z]+(?: [A-Za-z]+)*$")

    fun validate(
        line1: String,
        line2: String,
        city: String,
        state: String,
        pincode: String
    ): AddressFieldErrors {
        return AddressFieldErrors(
            line1 = validateLine1(line1),
            line2 = validateLine2(line2),
            city = validateCity(city),
            state = validateState(state),
            pincode = validatePincode(pincode)
        )
    }

    private fun validateLine1(raw: String): Int? {
        val value = raw.trim()
        if (value.isEmpty()) return R.string.error_address_line1_required
        if (value.length < MIN_ADDRESS_LINE1) return R.string.error_address_line_too_short
        if (value.length > MAX_ADDRESS_LINE) return R.string.error_address_line_too_long
        if (!addressLineRegex.matches(value)) return R.string.error_address_line_invalid
        return null
    }

    private fun validateLine2(raw: String): Int? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        if (value.length > MAX_ADDRESS_LINE) return R.string.error_address_line_too_long
        if (!addressLineRegex.matches(value)) return R.string.error_address_line_invalid
        return null
    }

    private fun validateCity(raw: String): Int? {
        val value = raw.trim()
        if (value.isEmpty()) return R.string.error_city_required
        if (value.length < MIN_CITY) return R.string.error_city_too_short
        if (value.length > MAX_CITY) return R.string.error_city_too_long
        if (!cityRegex.matches(value)) return R.string.error_city_invalid
        return null
    }

    private fun validateState(raw: String): Int? {
        val value = raw.trim()
        if (value.isEmpty() || value == IndianStates.PLACEHOLDER) {
            return R.string.error_state_required
        }
        if (!IndianStates.isValid(value)) return R.string.error_state_required
        return null
    }

    private fun validatePincode(raw: String): Int? {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return R.string.error_pincode_required
        if (digits.length != PINCODE_LENGTH || digits.any { !it.isDigit() }) {
            return R.string.error_invalid_pincode
        }
        // Indian PIN codes do not start with 0.
        if (digits.startsWith("0")) return R.string.error_invalid_pincode
        return null
    }
}
