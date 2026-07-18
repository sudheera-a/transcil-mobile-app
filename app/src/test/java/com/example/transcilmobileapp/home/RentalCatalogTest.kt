package com.example.transcilmobileapp.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RentalCatalogTest {

    @Test
    fun models_areExactlyEllodEliteAndElacil25() {
        val ids = RentalCatalog.models().map { it.id }
        assertEquals(
            listOf(VehicleModelId.ELLOD_ELITE, VehicleModelId.ELACIL_2_5),
            ids
        )
    }

    @Test
    fun specs_matchKnowledgeBaseBatteryAndVoltage() {
        RentalCatalog.models().forEach { spec ->
            assertEquals(30, spec.batteryAh)
            assertEquals(60, spec.voltage)
        }
    }

    @Test
    fun prices_matchKnowledgeBasePaise() {
        assertEquals(154_900L, RentalCatalog.pricePaise(VehicleModelId.ELLOD_ELITE, PlanType.WEEKLY))
        assertEquals(590_000L, RentalCatalog.pricePaise(VehicleModelId.ELLOD_ELITE, PlanType.MONTHLY))
        assertEquals(179_900L, RentalCatalog.pricePaise(VehicleModelId.ELACIL_2_5, PlanType.WEEKLY))
        assertEquals(650_000L, RentalCatalog.pricePaise(VehicleModelId.ELACIL_2_5, PlanType.MONTHLY))
    }

    @Test
    fun onboardingFee_is2500NonRefundable() {
        val fee = RentalCatalog.onboardingFee()
        assertEquals(250_000L, fee.amountPaise)
        assertFalse(fee.refundable)
        assertEquals(250_000L, RentalCatalog.ONBOARDING_FEE_PAISE)
    }

    @Test
    fun defaultActiveModel_isEllodElite() {
        assertEquals(VehicleModelId.ELLOD_ELITE, RentalCatalog.defaultActiveModel().id)
    }

    @Test
    fun vehicleStatus_enumHasFleetLifecycleValues() {
        val names = VehicleStatus.entries.map { it.name }.toSet()
        assertTrue(names.containsAll(listOf("AVAILABLE", "RENTED", "IN_SERVICE", "RETIRED")))
    }
}
