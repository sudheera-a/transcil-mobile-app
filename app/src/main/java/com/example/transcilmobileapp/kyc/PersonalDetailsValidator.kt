package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.Gender
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class PersonalFieldErrors(
    val fullName: Int? = null,
    val email: Int? = null,
    val dateOfBirth: Int? = null,
    val gender: Int? = null
) {
    val hasErrors: Boolean
        get() = fullName != null || email != null || dateOfBirth != null || gender != null
}

object PersonalEmailDomains {
    val ALLOWED = setOf(
        "gmail.com",
        "googlemail.com",
        "yahoo.com",
        "yahoo.co.in",
        "outlook.com",
        "hotmail.com",
        "live.com",
        "icloud.com",
        "me.com",
        "aol.com",
        "proton.me",
        "protonmail.com",
        "zoho.com",
        "rediffmail.com"
    )
}

object PersonalDetailsValidator {

    private const val MIN_NAME_LENGTH = 2
    private const val MAX_NAME_LENGTH = 40
    private const val MIN_AGE = 18

    private val nameRegex = Regex("^[A-Za-z]+(?: [A-Za-z]+)*$")
    // Pure regex so unit tests do not depend on android.util.Patterns stubs.
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    private val dobFormat = SimpleDateFormat("dd - MM - yyyy", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun validate(
        fullName: String,
        email: String,
        dateOfBirth: String,
        gender: Gender?,
        todayMillis: Long = System.currentTimeMillis()
    ): PersonalFieldErrors {
        return PersonalFieldErrors(
            fullName = validateFullName(fullName),
            email = validateEmail(email),
            dateOfBirth = validateDateOfBirth(dateOfBirth, todayMillis),
            gender = if (gender == null) R.string.error_gender_required else null
        )
    }

    private fun validateFullName(raw: String): Int? {
        val name = raw.trim()
        if (name.isEmpty()) return R.string.error_full_name_required
        if (name.length < MIN_NAME_LENGTH) return R.string.error_full_name_too_short
        if (name.length > MAX_NAME_LENGTH) return R.string.error_full_name_too_long
        if (!nameRegex.matches(name)) return R.string.error_full_name_invalid
        return null
    }

    private fun validateEmail(raw: String): Int? {
        val value = raw.trim()
        if (value.isEmpty()) return R.string.error_email_required
        if (!emailRegex.matches(value)) {
            return R.string.error_invalid_email
        }
        val domain = value.substringAfter('@', missingDelimiterValue = "").lowercase(Locale.US)
        if (domain.isEmpty() || domain !in PersonalEmailDomains.ALLOWED) {
            return R.string.error_email_domain
        }
        return null
    }

    private fun validateDateOfBirth(raw: String, todayMillis: Long): Int? {
        val value = raw.trim()
        if (value.isEmpty()) return R.string.error_dob_required

        val birthDate = try {
            dobFormat.parse(value) ?: return R.string.error_dob_required
        } catch (_: ParseException) {
            return R.string.error_dob_required
        }

        val today = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = todayMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val birth = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = birthDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (birth.after(today)) return R.string.error_dob_future
        if (ageYears(birth, today) < MIN_AGE) return R.string.error_dob_underage
        return null
    }

    private fun ageYears(birth: Calendar, today: Calendar): Int {
        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        val monthDiff = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
        val dayDiff = today.get(Calendar.DAY_OF_MONTH) - birth.get(Calendar.DAY_OF_MONTH)
        if (monthDiff < 0 || (monthDiff == 0 && dayDiff < 0)) {
            age--
        }
        return age
    }
}
