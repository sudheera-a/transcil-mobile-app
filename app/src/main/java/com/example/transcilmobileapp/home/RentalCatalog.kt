package com.example.transcilmobileapp.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.transcilmobileapp.R

enum class VehicleModelId {
    ELLOD_ELITE,
    ELACIL_2_5
}

enum class PlanType {
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

    private val specs = listOf(
        VehicleModelSpec(
            id = VehicleModelId.ELLOD_ELITE,
            displayNameRes = R.string.vehicle_model_ellod_elite,
            batteryAh = 30,
            voltage = 60,
            weeklyPricePaise = 154_900L,
            monthlyPricePaise = 590_000L,
            imageRes = R.drawable.scooter_onboarding,
        ),
        VehicleModelSpec(
            id = VehicleModelId.ELACIL_2_5,
            displayNameRes = R.string.vehicle_model_elacil_2_5,
            batteryAh = 30,
            voltage = 60,
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
            PlanType.WEEKLY -> spec.weeklyPricePaise
            PlanType.MONTHLY -> spec.monthlyPricePaise
        }
    }

    fun defaultActiveModel(): VehicleModelSpec = model(VehicleModelId.ELLOD_ELITE)

    fun onboardingFee(): OnboardingFee =
        OnboardingFee(amountPaise = ONBOARDING_FEE_PAISE, refundable = false)
}
