/**
 * App-level KYC outcome used for navigation and dashboard entry (pending vs approved).
 * Distinct from server `kyc_status` strings in onboarding DTOs — this is the UI routing enum.
 *
 * Kotlin notes:
 * - `enum class` = closed set; exhaustive `when (status)` without an else branch.
 */
package com.transcil.rider.core

enum class KycStatus {
    PENDING,
    APPROVED
}
