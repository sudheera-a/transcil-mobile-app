package com.example.transcilmobileapp.repository

import com.example.transcilmobileapp.core.Gender
import com.example.transcilmobileapp.core.JourneyType
import com.example.transcilmobileapp.data.model.ApiResponse
import com.example.transcilmobileapp.data.model.onboarding.*
import com.example.transcilmobileapp.data.network.FakeTranscilApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRepositoryTest {

    @Test
    fun roleMapping_roundTrip() {
        assertEquals("rider", JourneyType.RENT_EV.toRiderRole())
        assertEquals("3pl", JourneyType.THREE_PL.toRiderRole())
        assertEquals(JourneyType.RENT_EV, riderRoleToJourney("rider"))
        assertEquals(JourneyType.THREE_PL, riderRoleToJourney("3pl"))
    }

    @Test
    fun toApiDob_convertsUiFormat() {
        assertEquals("1996-06-16", toApiDob("16 - 06 - 1996"))
    }

    @Test
    fun setRiderRole_sendsRiderRole() = runBlocking {
        var captured: RiderRoleRequest? = null
        val api = object : FakeTranscilApi() {
            override suspend fun setRiderRole(body: RiderRoleRequest): ApiResponse<RiderRoleData> {
                captured = body
                return ApiResponse(RiderRoleData(riderRole = body.riderRole), null, null)
            }
        }
        val result = OnboardingRepository(api).setRiderRole(JourneyType.THREE_PL)
        assertTrue(result.isSuccess)
        assertEquals("3pl", captured?.riderRole)
    }

    @Test
    fun patchProfile_mapsPersonalFields() = runBlocking {
        var captured: ProfilePatchRequest? = null
        val api = object : FakeTranscilApi() {
            override suspend fun patchProfile(
                idempotencyKey: String,
                body: ProfilePatchRequest,
            ): ApiResponse<ProfileData> {
                captured = body
                return ApiResponse(ProfileData(displayName = body.displayName), null, null)
            }
        }
        val result = OnboardingRepository(api).patchProfile(
            fullName = "Ravi Kumar",
            email = "ravi@example.com",
            uiDob = "01 - 01 - 1990",
            gender = Gender.MALE,
        )
        assertTrue(result.isSuccess)
        assertEquals("Ravi Kumar", captured?.displayName)
        assertEquals("Ravi", captured?.givenName)
        assertEquals("ravi@example.com", captured?.email)
        assertEquals("1990-01-01", captured?.dob)
        assertEquals("male", captured?.gender)
    }

    @Test
    fun listStates_mapsCodeAndName() = runBlocking {
        val api = object : FakeTranscilApi() {
            override suspend fun getStates(): ApiResponse<List<ReferenceStateDto>> =
                ApiResponse(
                    listOf(
                        ReferenceStateDto(code = "TG", name = "Telangana", sortOrder = 2),
                        ReferenceStateDto(code = "KA", name = "Karnataka", sortOrder = 1),
                    ),
                    null,
                    null,
                )
        }
        val result = OnboardingRepository(api).listStates()
        assertTrue(result.isSuccess)
        assertEquals(listOf("KA", "TG"), result.getOrNull()?.map { it.code })
        assertEquals("Karnataka", result.getOrNull()?.first()?.name)
    }

    @Test
    fun putAddress_trimsAndDigits_sendsIdempotencyKey() = runBlocking {
        var captured: AddressUpsertRequest? = null
        var key: String? = null
        val api = object : FakeTranscilApi() {
            override suspend fun putAddress(
                idempotencyKey: String,
                body: AddressUpsertRequest,
            ): ApiResponse<AddressData> {
                key = idempotencyKey
                captured = body
                return ApiResponse(AddressData(city = body.city), null, null)
            }
        }
        val result = OnboardingRepository(api).putAddress(
            line1 = "  Line 1 ",
            line2 = "  ",
            city = " Hyderabad ",
            state = "Telangana",
            pincode = "500001x",
        )
        assertTrue(result.isSuccess)
        assertEquals("Line 1", captured?.addressLine1)
        assertEquals(null, captured?.addressLine2)
        assertEquals("Hyderabad", captured?.city)
        assertEquals("500001", captured?.pincode)
        assertTrue(!key.isNullOrBlank())
    }

    @Test
    fun putReference_mapsE164AndRelation() = runBlocking {
        var captured: com.example.transcilmobileapp.data.model.kyc.ReferenceUpsertRequest? = null
        var key: String? = null
        val api = object : FakeTranscilApi() {
            override suspend fun putReference(
                idempotencyKey: String,
                body: com.example.transcilmobileapp.data.model.kyc.ReferenceUpsertRequest,
            ): ApiResponse<com.example.transcilmobileapp.data.model.kyc.ReferenceData> {
                key = idempotencyKey
                captured = body
                return ApiResponse(
                    com.example.transcilmobileapp.data.model.kyc.ReferenceData(relation = body.relation),
                    null,
                    null,
                )
            }
        }
        val result = OnboardingRepository(api).putReference("Mother", "9876543210")
        assertTrue(result.isSuccess)
        assertEquals("mother", captured?.relation)
        assertEquals("+919876543210", captured?.mobileE164)
        assertTrue(!key.isNullOrBlank())
    }
}
