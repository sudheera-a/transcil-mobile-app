/**
 * Static list of Indian states and union territories for address dropdowns and offline validation.
 * Used when the states API is unavailable and as a placeholder label for the state spinner.
 */
package com.transcil.rider.kyc

/** `object`: read-only catalog — `val ALL` is the canonical ordered list. */
object IndianStates {

    const val PLACEHOLDER = "Select state"

    val ALL: List<String> = listOf(
        "Andaman and Nicobar Islands",
        "Andhra Pradesh",
        "Arunachal Pradesh",
        "Assam",
        "Bihar",
        "Chandigarh",
        "Chhattisgarh",
        "Dadra and Nagar Haveli and Daman and Diu",
        "Delhi",
        "Goa",
        "Gujarat",
        "Haryana",
        "Himachal Pradesh",
        "Jammu and Kashmir",
        "Jharkhand",
        "Karnataka",
        "Kerala",
        "Ladakh",
        "Lakshadweep",
        "Madhya Pradesh",
        "Maharashtra",
        "Manipur",
        "Meghalaya",
        "Mizoram",
        "Nagaland",
        "Odisha",
        "Puducherry",
        "Punjab",
        "Rajasthan",
        "Sikkim",
        "Tamil Nadu",
        "Telangana",
        "Tripura",
        "Uttar Pradesh",
        "Uttarakhand",
        "West Bengal"
    )

    val ALL_WITH_PLACEHOLDER: List<String> = listOf(PLACEHOLDER) + ALL

    fun isValid(state: String): Boolean = state.trim() in ALL

    fun indexOf(state: String): Int {
        val trimmed = state.trim()
        if (trimmed.isEmpty()) return 0
        val index = ALL.indexOf(trimmed)
        return if (index >= 0) index + 1 else 0
    }
}
