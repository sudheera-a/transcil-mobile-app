package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PersonalDetailsValidatorTest {

    private val todayMillis = utcMillis(2026, Calendar.JULY, 18)

    @Test
    fun validInput_hasNoErrors() {
        val errors = PersonalDetailsValidator.validate(
            fullName = "Sai Kumar",
            email = "sai.kumar@gmail.com",
            dateOfBirth = "18 - 07 - 2000",
            gender = Gender.MALE,
            todayMillis = todayMillis
        )
        assertFalse(errors.hasErrors)
        assertNull(errors.fullName)
        assertNull(errors.email)
        assertNull(errors.dateOfBirth)
        assertNull(errors.gender)
    }

    @Test
    fun fullName_blank_required() {
        val errors = validateName("")
        assertEquals(R.string.error_full_name_required, errors.fullName)
    }

    @Test
    fun fullName_tooShort() {
        val errors = validateName("A")
        assertEquals(R.string.error_full_name_too_short, errors.fullName)
    }

    @Test
    fun fullName_tooLong() {
        val errors = validateName("A".repeat(41))
        assertEquals(R.string.error_full_name_too_long, errors.fullName)
    }

    @Test
    fun fullName_invalidChars() {
        val errors = validateName("Sai123")
        assertEquals(R.string.error_full_name_invalid, errors.fullName)
    }

    @Test
    fun fullName_maxLengthAllowed() {
        val errors = validateName("A".repeat(40))
        assertNull(errors.fullName)
    }

    @Test
    fun email_blank_required() {
        val errors = validateEmail("")
        assertEquals(R.string.error_email_required, errors.email)
    }

    @Test
    fun email_invalidFormat() {
        val errors = validateEmail("not-an-email")
        assertEquals(R.string.error_invalid_email, errors.email)
    }

    @Test
    fun email_disallowedDomain() {
        val errors = validateEmail("user@company.com")
        assertEquals(R.string.error_email_domain, errors.email)
    }

    @Test
    fun email_allowedDomain_caseInsensitive() {
        val errors = validateEmail("User@Gmail.COM")
        assertNull(errors.email)
    }

    @Test
    fun dob_blank_required() {
        val errors = validateDob("")
        assertEquals(R.string.error_dob_required, errors.dateOfBirth)
    }

    @Test
    fun dob_future_rejected() {
        val errors = validateDob("19 - 07 - 2026")
        assertEquals(R.string.error_dob_future, errors.dateOfBirth)
    }

    @Test
    fun dob_underage_rejected() {
        val errors = validateDob("19 - 07 - 2008")
        assertEquals(R.string.error_dob_underage, errors.dateOfBirth)
    }

    @Test
    fun dob_exactlyEighteen_allowed() {
        val errors = validateDob("18 - 07 - 2008")
        assertNull(errors.dateOfBirth)
    }

    @Test
    fun gender_null_required() {
        val errors = PersonalDetailsValidator.validate(
            fullName = "Sai Kumar",
            email = "sai@gmail.com",
            dateOfBirth = "18 - 07 - 2000",
            gender = null,
            todayMillis = todayMillis
        )
        assertEquals(R.string.error_gender_required, errors.gender)
    }

    @Test
    fun multipleFields_failTogether() {
        val errors = PersonalDetailsValidator.validate(
            fullName = "",
            email = "bad",
            dateOfBirth = "",
            gender = null,
            todayMillis = todayMillis
        )
        assertTrue(errors.hasErrors)
        assertEquals(R.string.error_full_name_required, errors.fullName)
        assertEquals(R.string.error_invalid_email, errors.email)
        assertEquals(R.string.error_dob_required, errors.dateOfBirth)
        assertEquals(R.string.error_gender_required, errors.gender)
    }

    private fun validateName(name: String) = PersonalDetailsValidator.validate(
        fullName = name,
        email = "sai@gmail.com",
        dateOfBirth = "18 - 07 - 2000",
        gender = Gender.MALE,
        todayMillis = todayMillis
    )

    private fun validateEmail(email: String) = PersonalDetailsValidator.validate(
        fullName = "Sai Kumar",
        email = email,
        dateOfBirth = "18 - 07 - 2000",
        gender = Gender.MALE,
        todayMillis = todayMillis
    )

    private fun validateDob(dob: String) = PersonalDetailsValidator.validate(
        fullName = "Sai Kumar",
        email = "sai@gmail.com",
        dateOfBirth = dob,
        gender = Gender.MALE,
        todayMillis = todayMillis
    )

    private fun utcMillis(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
