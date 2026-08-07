/**
 * Domain enums for onboarding UI choices (journey type, gender).
 * Kept in core (not data/model) because they represent app concepts, not raw JSON shapes.
 *
 * Kotlin notes:
 * - `enum class` = type-safe alternatives to magic strings in forms and API mappers.
 */
package com.transcil.rider.core

enum class JourneyType {
    RENT_EV,
    THREE_PL
}

enum class Gender {
    MALE,
    FEMALE,
    OTHER
}
