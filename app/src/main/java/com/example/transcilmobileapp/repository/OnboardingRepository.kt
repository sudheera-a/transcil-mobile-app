package com.example.transcilmobileapp.repository

import com.example.transcilmobileapp.core.Gender
import com.example.transcilmobileapp.core.JourneyType
import com.example.transcilmobileapp.data.model.kyc.PanVerifyData
import com.example.transcilmobileapp.data.model.kyc.PanVerifyRequest
import com.example.transcilmobileapp.data.model.kyc.ReferenceData
import com.example.transcilmobileapp.data.model.kyc.ReferenceUpsertRequest
import com.example.transcilmobileapp.data.model.onboarding.*
import com.example.transcilmobileapp.data.network.TranscilApi
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

fun JourneyType.toRiderRole(): String = when (this) {
    JourneyType.RENT_EV -> "rider"
    JourneyType.THREE_PL -> "3pl"
}

fun riderRoleToJourney(role: String?): JourneyType? = when (role?.lowercase(Locale.US)) {
    "rider" -> JourneyType.RENT_EV
    "3pl" -> JourneyType.THREE_PL
    else -> null
}

fun Gender.toApiGender(): String = name.lowercase(Locale.US)

/** UI `dd - MM - yyyy` → API `yyyy-MM-dd`. */
fun toApiDob(uiDob: String): String {
    val utc = TimeZone.getTimeZone("UTC")
    val input = SimpleDateFormat("dd - MM - yyyy", Locale.US).apply {
        isLenient = false
        timeZone = utc
    }
    val output = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }
    val parsed = input.parse(uiDob.trim()) ?: error("Invalid date of birth")
    return output.format(parsed)
}

fun givenNameFrom(fullName: String): String =
    fullName.trim().split(Regex("\\s+")).firstOrNull().orEmpty()

fun fromApiDob(apiDob: String): String {
    val utc = TimeZone.getTimeZone("UTC")
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = utc
    }
    val output = SimpleDateFormat("dd - MM - yyyy", Locale.US).apply { timeZone = utc }
    return runCatching { output.format(input.parse(apiDob.trim())!!) }.getOrDefault(apiDob)
}

fun parseApiGender(raw: String?): Gender? = when (raw?.lowercase(Locale.US)) {
    "male" -> Gender.MALE
    "female" -> Gender.FEMALE
    "other", "prefer_not" -> Gender.OTHER
    else -> null
}

class OnboardingRepository(
    private val api: TranscilApi = com.example.transcilmobileapp.data.network.ApiClient.transcilApi,
) {
    suspend fun journeyOptions(): Result<List<JourneyOptionDto>> = runCatching {
        val res = api.getJourneyOptions()
        res.error?.let { error(it.message ?: it.code ?: "JOURNEY_OPTIONS_FAILED") }
        res.data ?: emptyList()
    }

    suspend fun setRiderRole(journey: JourneyType): Result<RiderRoleData> = runCatching {
        val res = api.setRiderRole(RiderRoleRequest(journey.toRiderRole()))
        res.error?.let { error(it.message ?: it.code ?: "RIDER_ROLE_FAILED") }
        res.data ?: error("RIDER_ROLE_EMPTY")
    }

    suspend fun getProfile(): Result<ProfileData> = runCatching {
        val res = api.getProfile()
        res.error?.let { error(it.message ?: it.code ?: "PROFILE_GET_FAILED") }
        res.data ?: error("PROFILE_EMPTY")
    }

    suspend fun patchProfile(
        fullName: String,
        email: String,
        uiDob: String,
        gender: Gender,
    ): Result<ProfileData> = runCatching {
        val name = fullName.trim()
        val body = ProfilePatchRequest(
            displayName = name,
            givenName = givenNameFrom(name),
            email = email.trim(),
            dob = toApiDob(uiDob),
            gender = gender.toApiGender(),
        )
        val res = api.patchProfile(UUID.randomUUID().toString(), body)
        res.error?.let { error(it.message ?: it.code ?: "PROFILE_PATCH_FAILED") }
        res.data ?: error("PROFILE_PATCH_EMPTY")
    }

    suspend fun getAddress(): Result<AddressData> = runCatching {
        val res = api.getAddress()
        res.error?.let { error(it.message ?: it.code ?: "ADDRESS_GET_FAILED") }
        res.data ?: error("ADDRESS_EMPTY")
    }

    suspend fun putAddress(
        line1: String,
        line2: String,
        city: String,
        state: String,
        pincode: String,
    ): Result<AddressData> = runCatching {
        val body = AddressUpsertRequest(
            addressLine1 = line1.trim(),
            addressLine2 = line2.trim().ifBlank { null },
            city = city.trim(),
            state = state.trim(),
            pincode = pincode.filter { it.isDigit() }.take(6),
        )
        val res = api.putAddress(UUID.randomUUID().toString(), body)
        res.error?.let { error(it.message ?: it.code ?: "ADDRESS_PUT_FAILED") }
        res.data ?: error("ADDRESS_PUT_EMPTY")
    }

    suspend fun getOnboarding(): Result<OnboardingData> = runCatching {
        val res = api.getOnboarding()
        res.error?.let { error(it.message ?: it.code ?: "ONBOARDING_GET_FAILED") }
        res.data ?: error("ONBOARDING_EMPTY")
    }

    suspend fun putReference(relationLabel: String, mobile10: String): Result<ReferenceData> =
        runCatching {
            val digits = mobile10.filter { it.isDigit() }.takeLast(10)
            require(digits.length == 10) { "INVALID_MOBILE" }
            val body = ReferenceUpsertRequest(
                relation = relationLabel.trim().lowercase(Locale.US),
                mobileE164 = "+91$digits",
            )
            val res = api.putReference(UUID.randomUUID().toString(), body)
            res.error?.let { error(it.message ?: it.code ?: "REFERENCE_PUT_FAILED") }
            res.data ?: error("REFERENCE_PUT_EMPTY")
        }

    suspend fun getReference(): Result<ReferenceData> = runCatching {
        val res = api.getReference()
        res.error?.let { error(it.message ?: it.code ?: "REFERENCE_GET_FAILED") }
        res.data ?: error("REFERENCE_EMPTY")
    }

    suspend fun verifyPan(
        panNumber: String,
        name: String,
        uiDob: String? = null,
    ): Result<PanVerifyData> = runCatching {
        val body = PanVerifyRequest(
            panNumber = panNumber.trim().uppercase(Locale.US),
            name = name.trim(),
            dob = uiDob?.takeIf { it.isNotBlank() }?.let { toApiDob(it) },
        )
        val res = api.verifyPan(UUID.randomUUID().toString(), body)
        res.error?.let { error(it.message ?: it.code ?: "PAN_VERIFY_FAILED") }
        res.data ?: error("PAN_VERIFY_EMPTY")
    }

    suspend fun listStates(): Result<List<StateOption>> = runCatching {
        val res = api.getStates()
        res.error?.let { error(it.message ?: it.code ?: "STATES_FAILED") }
        (res.data ?: emptyList())
            .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
            .map { StateOption(code = it.code, name = it.name) }
    }

    suspend fun listCities(stateCode: String): Result<List<String>> = runCatching {
        val code = stateCode.trim()
        if (code.isEmpty()) return@runCatching emptyList()
        val res = api.getCities(code)
        res.error?.let { error(it.message ?: it.code ?: "CITIES_FAILED") }
        (res.data ?: emptyList())
            .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
            .map { it.name }
            .filter { it.isNotBlank() }
    }
}
