package com.example.transcilmobileapp.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.transcilmobileapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun notificationsEnabled_defaultsTrue_andToggles() {
        val vm = SettingsViewModel()
        assertTrue(vm.notificationsEnabled.value == true)
        vm.onNotificationsToggled(false)
        assertFalse(vm.notificationsEnabled.value == true)
        vm.onNotificationsToggled(true)
        assertTrue(vm.notificationsEnabled.value == true)
    }

    @Test
    fun stubActions_emitToast_thenClear() {
        val vm = SettingsViewModel()
        vm.onLanguage()
        assertEquals(R.string.settings_item_stub, vm.toastMessage.value)
        vm.clearToast()
        assertNull(vm.toastMessage.value)

        vm.onLogout()
        assertEquals(R.string.settings_logout_stub, vm.toastMessage.value)
        vm.clearToast()
        assertNull(vm.toastMessage.value)
    }
}
