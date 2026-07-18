package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.R

data class BankFieldErrors(
    val holderName: Int? = null,
    val accountNumber: Int? = null,
    val confirmAccountNumber: Int? = null,
    val ifsc: Int? = null,
    val consent: Int? = null
) {
    val hasErrors: Boolean
        get() = holderName != null ||
            accountNumber != null ||
            confirmAccountNumber != null ||
            ifsc != null ||
            consent != null
}

object BankDetailsValidator {

    private const val MIN_ACCOUNT_LENGTH = 9
    private const val MAX_ACCOUNT_LENGTH = 18
    private val holderNameRegex = Regex("^[A-Za-z]+(?: [A-Za-z]+)*$")
    private val ifscRegex = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")

    fun validate(
        holderName: String,
        accountNumber: String,
        confirmAccountNumber: String,
        ifsc: String,
        consent: Boolean
    ): BankFieldErrors {
        val account = accountNumber.filter { it.isDigit() }
        val confirm = confirmAccountNumber.filter { it.isDigit() }
        val normalizedIfsc = ifsc.trim().uppercase()

        val holderError = validateHolder(holderName)
        val accountError = validateAccount(account)
        val confirmError = when {
            confirm.isEmpty() -> R.string.error_confirm_account_required
            accountError == null && account != confirm -> R.string.error_account_mismatch
            else -> null
        }
        val ifscError = when {
            normalizedIfsc.isEmpty() -> R.string.error_ifsc_required
            !ifscRegex.matches(normalizedIfsc) -> R.string.error_invalid_ifsc
            else -> null
        }
        val consentError = if (!consent) R.string.error_bank_consent else null

        return BankFieldErrors(
            holderName = holderError,
            accountNumber = accountError,
            confirmAccountNumber = confirmError,
            ifsc = ifscError,
            consent = consentError
        )
    }

    private fun validateHolder(raw: String): Int? {
        val value = raw.trim()
        if (value.isEmpty()) return R.string.error_account_holder_required
        if (value.length < 2) return R.string.error_account_holder_invalid
        if (value.length > 40) return R.string.error_account_holder_invalid
        if (!holderNameRegex.matches(value)) return R.string.error_account_holder_invalid
        return null
    }

    private fun validateAccount(digits: String): Int? {
        if (digits.isEmpty()) return R.string.error_account_number_required
        if (digits.length !in MIN_ACCOUNT_LENGTH..MAX_ACCOUNT_LENGTH) {
            return R.string.error_account_number_invalid
        }
        return null
    }
}
