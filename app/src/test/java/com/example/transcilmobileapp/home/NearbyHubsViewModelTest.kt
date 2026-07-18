package com.example.transcilmobileapp.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.transcilmobileapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NearbyHubsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun stations_loadFromRepository() {
        val vm = NearbyHubsViewModel()
        val stations = vm.stations.value.orEmpty()
        assertTrue(stations.isNotEmpty())
        assertTrue(stations.any { it.status == StationStatus.PENDING })
        assertTrue(stations.any { it.status == StationStatus.ACTIVE })
    }

    @Test
    fun onNavigateClicked_setsMessageAndName_thenClears() {
        val vm = NearbyHubsViewModel()
        vm.onNavigateClicked("Nagole Swap Station")
        assertEquals(R.string.nearby_hubs_navigate_stub, vm.navigateMessage.value)
        assertEquals("Nagole Swap Station", vm.navigateStationName.value)

        vm.clearNavigateMessage()
        assertNull(vm.navigateMessage.value)
        assertNull(vm.navigateStationName.value)
    }
}
