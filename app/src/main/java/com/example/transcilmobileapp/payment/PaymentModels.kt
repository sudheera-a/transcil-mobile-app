package com.example.transcilmobileapp.payment

enum class PaymentStep {
    REVIEW,
    AUTOPAY,
    PENDING,
    SUCCESS,
    FAILURE,
    METHODS,
}

data class PaymentBreakdown(
    val rentPaise: Long,
    val onboardingPaise: Long,
    val depositPaise: Long,
) {
    val totalPaise: Long get() = rentPaise + onboardingPaise + depositPaise
}
