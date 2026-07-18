package com.example.transcilmobileapp.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.transcilmobileapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BatterySwapViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun overviewAndRecentSwaps_loadFromRepository() {
        val vm = BatterySwapViewModel()
        assertEquals(72, vm.overview.value?.percent)
        assertEquals(58, vm.overview.value?.rangeKm)
        assertTrue(vm.recentSwaps.value.orEmpty().size >= 3)
    }

    @Test
    fun onScanQr_emitsToastAndEvent_thenClears() {
        val vm = BatterySwapViewModel()
        vm.onScanQr()
        assertEquals(R.string.battery_swap_scan_stub, vm.toastMessage.value)
        assertEquals(BatterySwapEvent.ScanQr, vm.event.value)

        vm.clearToast()
        vm.clearEvent()
        assertNull(vm.toastMessage.value)
        assertNull(vm.event.value)
    }

    @Test
    fun onFindStation_emitsFindStationEvent() {
        val vm = BatterySwapViewModel()
        vm.onFindStation()
        assertEquals(BatterySwapEvent.FindStation, vm.event.value)
        vm.clearEvent()
        assertNull(vm.event.value)
    }
}
