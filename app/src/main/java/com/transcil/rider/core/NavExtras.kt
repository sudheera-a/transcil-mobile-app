/**
 * Shared Intent extra keys for passing data between Activities in the auth/onboarding flow.
 * Using named constants avoids typos in putExtra/getStringExtra string literals.
 *
 * Kotlin notes:
 * - `object` with `const val` = namespace for compile-time string keys (no instance required).
 */
package com.transcil.rider.core

object NavExtras {
    const val MOBILE_NUMBER = "MOBILE_NUMBER"
    const val OTP_SESSION = "OTP_SESSION"
    const val JOURNEY_TYPE = "JOURNEY_TYPE"
}
