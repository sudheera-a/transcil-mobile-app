package com.example.transcilmobileapp.data.model.onboarding

import com.google.gson.annotations.SerializedName

data class JourneyOptionDto(
    @SerializedName("role_key") val roleKey: String,
    val title: String? = null,
    val subtitle: String? = null,
    @SerializedName("icon_key") val iconKey: String? = null,
    val features: List<String>? = null,
)

data class RiderRoleRequest(
    @SerializedName("rider_role") val riderRole: String,
)

data class RiderRoleData(
    @SerializedName("rider_id") val riderId: String? = null,
    @SerializedName("rider_role") val riderRole: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
)

data class ProfilePatchRequest(
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("given_name") val givenName: String? = null,
    val email: String? = null,
    val dob: String? = null,
    val gender: String? = null,
)

data class ProfileData(
    @SerializedName("rider_id") val riderId: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("given_name") val givenName: String? = null,
    @SerializedName("family_name") val familyName: String? = null,
    val email: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    @SerializedName("phone_e164") val phoneE164: String? = null,
    @SerializedName("address_line1") val addressLine1: String? = null,
    @SerializedName("address_line2") val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    @SerializedName("all_complete") val allComplete: Boolean? = null,
    @SerializedName("kyc_status") val kycStatus: String? = null,
)

data class AddressUpsertRequest(
    @SerializedName("address_line1") val addressLine1: String,
    @SerializedName("address_line2") val addressLine2: String? = null,
    val city: String,
    val state: String,
    val pincode: String,
)

data class AddressData(
    @SerializedName("rider_id") val riderId: String? = null,
    @SerializedName("address_line1") val addressLine1: String? = null,
    @SerializedName("address_line2") val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
)

data class OnboardingData(
    @SerializedName("rider_role") val riderRole: String? = null,
    @SerializedName("overall_percent") val overallPercent: Int = 0,
    @SerializedName("completed_steps") val completedSteps: Int = 0,
    @SerializedName("total_steps") val totalSteps: Int = 0,
    @SerializedName("all_complete") val allComplete: Boolean = false,
    val documents: OnboardingDocumentsStatus? = null,
    val steps: List<OnboardingStepDto> = emptyList(),
)

data class OnboardingDocumentsStatus(
    val overall: String? = null,
    val verified: Boolean = false,
)

data class OnboardingStepDto(
    val key: String,
    val label: String? = null,
    val status: String,
    @SerializedName("completed_at") val completedAt: String? = null,
    val provider: String? = null,
    @SerializedName("consent_required") val consentRequired: Boolean = false,
    @SerializedName("consent_captured") val consentCaptured: Boolean = false,
    @SerializedName("edit_endpoint") val editEndpoint: String? = null,
    val fields: Map<String, String>? = null,
)

data class ReferenceStateDto(
    val code: String,
    val name: String,
    @SerializedName(value = "sortOrder", alternate = ["sort_order"])
    val sortOrder: Int? = null,
)

data class ReferenceCityDto(
    val id: String? = null,
    @SerializedName(value = "stateCode", alternate = ["state_code"])
    val stateCode: String? = null,
    val name: String,
    @SerializedName(value = "sortOrder", alternate = ["sort_order"])
    val sortOrder: Int? = null,
)

/** Spinner/UI row for reference states (name shown, code submitted). */
data class StateOption(
    val code: String,
    val name: String,
) {
    override fun toString(): String = name
}
