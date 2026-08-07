/**
 * In-app vehicle catalog: models, plan types, pricing helpers for home and rental flows.
 */
package com.transcil.rider.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.transcil.rider.R

enum class VehicleModelId {
    ELLOD_ELITE,
    ELACIL_2_5
}

enum class PlanType {
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class VehicleStatus {
    AVAILABLE,
    RENTED,
    IN_SERVICE,
    RETIRED
}

data class VehicleModelSpec(
    val id: VehicleModelId,
    @param:StringRes val displayNameRes: Int,
    val batteryAh: Int,
    val voltage: Int,
    val dailyPricePaise: Long,
    val weeklyPricePaise: Long,
    val monthlyPricePaise: Long,
    @param:DrawableRes val imageRes: Int,
)

data class OnboardingFee(
    val amountPaise: Long,
    val refundable: Boolean,
)

object RentalCatalog {
    const val ONBOARDING_FEE_PAISE = 250_000L

    // private val = read-only reference; catalog list is not reassigned after init.
    private val specs = listOf(
        VehicleModelSpec(
            id = VehicleModelId.ELLOD_ELITE,
            displayNameRes = R.string.vehicle_model_ellod_elite,
            batteryAh = 30,
            voltage = 60,
            dailyPricePaise = 24_900L,
            weeklyPricePaise = 154_900L,
            monthlyPricePaise = 520_000L,
            imageRes = R.drawable.scooter_onboarding,
        ),
        VehicleModelSpec(
            id = VehicleModelId.ELACIL_2_5,
            displayNameRes = R.string.vehicle_model_elacil_2_5,
            batteryAh = 30,
            voltage = 60,
            dailyPricePaise = 24_900L,
            weeklyPricePaise = 179_900L,
            monthlyPricePaise = 650_000L,
            imageRes = R.drawable.scooter_onboarding,
        ),
    )

    fun models(): List<VehicleModelSpec> = specs

    fun model(id: VehicleModelId): VehicleModelSpec =
        specs.first { it.id == id }

    fun pricePaise(id: VehicleModelId, plan: PlanType): Long {
        val spec = model(id)
        return when (plan) {
            PlanType.DAILY -> spec.dailyPricePaise
            PlanType.WEEKLY -> spec.weeklyPricePaise
            PlanType.MONTHLY -> spec.monthlyPricePaise
        }
    }

    /** Normalised rupees-per-day for plan comparison UI. */
    fun perDayRupees(id: VehicleModelId, plan: PlanType): Long {
        val paise = pricePaise(id, plan)
        val days = when (plan) {
            PlanType.DAILY -> 1
            PlanType.WEEKLY -> 7
            PlanType.MONTHLY -> 30
        }
        return (paise / 100) / days
    }

    const val SECURITY_DEPOSIT_PAISE = 200_000L

    fun defaultActiveModel(): VehicleModelSpec = model(VehicleModelId.ELLOD_ELITE)

    fun onboardingFee(): OnboardingFee =
        OnboardingFee(amountPaise = ONBOARDING_FEE_PAISE, refundable = false)
}
