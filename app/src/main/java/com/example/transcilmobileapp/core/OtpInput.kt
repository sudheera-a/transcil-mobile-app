package com.example.transcilmobileapp.core

/** Pure OTP helpers — SMS parse + backspace focus math. */
object OtpInput {
    private val sixDigits = Regex("""(?<!\d)(\d{6})(?!\d)""")

    fun extractSixDigitCode(message: String): String? =
        sixDigits.find(message)?.groupValues?.get(1)

    /** Empty focused box + delete → clear previous index, or null if nothing to do. */
    fun previousIndexOnEmptyDelete(focusedIndex: Int, isEmpty: Boolean): Int? =
        if (isEmpty && focusedIndex > 0) focusedIndex - 1 else null
}
