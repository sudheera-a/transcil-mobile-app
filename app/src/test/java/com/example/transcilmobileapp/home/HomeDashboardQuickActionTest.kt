package com.example.transcilmobileapp.home

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure mapping of home quick actions → shell tabs.
 * Mirrors [HomeDashboardViewModel.onQuickAction] without AndroidViewModel.
 */
class HomeDashboardQuickActionTest {

    @Test
    fun batterySwap_mapsToBatteryTab() {
        assertEquals(HomeNavTab.BATTERY, tabFor(HomeQuickAction.BATTERY_SWAP))
    }

    @Test
    fun navigateAndHubs_mapToMapTab() {
        assertEquals(HomeNavTab.MAP, tabFor(HomeQuickAction.NAVIGATE))
        assertEquals(HomeNavTab.MAP, tabFor(HomeQuickAction.NEARBY_HUBS))
    }

    @Test
    fun extendRental_hasNoTabNavigation() {
        assertEquals(null, tabFor(HomeQuickAction.EXTEND_RENTAL))
    }

    private fun tabFor(action: HomeQuickAction): HomeNavTab? = when (action) {
        HomeQuickAction.BATTERY_SWAP -> HomeNavTab.BATTERY
        HomeQuickAction.NAVIGATE, HomeQuickAction.NEARBY_HUBS -> HomeNavTab.MAP
        HomeQuickAction.EXTEND_RENTAL -> null
    }
}
