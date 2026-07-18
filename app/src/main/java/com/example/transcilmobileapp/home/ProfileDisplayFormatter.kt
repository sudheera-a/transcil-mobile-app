package com.example.transcilmobileapp.home

/**
 * Pure formatting for Profile fields sourced from KYC session drafts.
 */
object ProfileDisplayFormatter {

    const val EMPTY = "—"

    fun formatPhone(mobileDigits: String): String {
        val digits = mobileDigits.filter { it.isDigit() }.takeLast(10)
        return if (digits.length == 10) "+91 $digits" else EMPTY
    }

    fun formatEmail(email: String): String = email.trim().ifBlank { EMPTY }

    fun formatLocation(city: String, state: String): String {
        val parts = listOf(city.trim(), state.trim()).filter { it.isNotBlank() }
        return if (parts.isEmpty()) EMPTY else parts.joinToString(", ")
    }
}
