/**
 * Payment flow step enum and fee breakdown model for the rental checkout demo.
 * Amounts are in paise (1 rupee = 100 paise) to avoid floating-point rounding issues.
 */
package com.transcil.rider.payment

/** `enum class`: drives which panel PaymentActivity displays. */
enum class PaymentStep {
    REVIEW,
    AUTOPAY,
    PENDING,
    SUCCESS,
    FAILURE,
    METHODS,
}

/** `data class`: rent + onboarding + deposit; [totalPaise] computed property sums components. */
data class PaymentBreakdown(
    val rentPaise: Long,
    val onboardingPaise: Long,
    val depositPaise: Long,
) {
    val totalPaise: Long get() = rentPaise + onboardingPaise + depositPaise
}
