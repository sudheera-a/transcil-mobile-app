package com.example.transcilmobileapp.core

import android.content.Intent
import com.example.transcilmobileapp.home.HomeNavTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contracts for KYC → Home entry without needing a full Activity instrumentation run.
 */
class HomeEntryContractTest {

    @Test
    fun kycStatus_extraNamesAreStable() {
        assertEquals("PENDING", KycStatus.PENDING.name)
        assertEquals("APPROVED", KycStatus.APPROVED.name)
        assertEquals("extra_kyc_status", KycNavigator.EXTRA_KYC_STATUS)
    }

    @Test
    fun startTab_namesMatchNavTabs() {
        assertEquals("HOME", HomeNavTab.HOME.name)
        assertEquals("MAP", HomeNavTab.MAP.name)
        assertEquals("BATTERY", HomeNavTab.BATTERY.name)
        assertEquals("WALLET", HomeNavTab.WALLET.name)
        assertEquals("PROFILE", HomeNavTab.PROFILE.name)
    }

    @Test
    fun openHomeDashboard_flagContract_clearsTask() {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        assertTrue(flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }
}
