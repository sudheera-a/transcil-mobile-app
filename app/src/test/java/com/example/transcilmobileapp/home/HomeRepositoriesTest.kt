package com.example.transcilmobileapp.home

import com.example.transcilmobileapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRepositoriesTest {

    @Test
    fun nearbyHubs_includePendingWithZeroAvailability() {
        val stations = NearbyHubsRepository.stations()
        assertTrue(stations.isNotEmpty())
        val pending = stations.first { it.status == StationStatus.PENDING }
        assertEquals(0, pending.available)
        assertTrue(pending.capacity > 0)
        assertTrue(stations.all { it.id.isNotBlank() })
    }

    @Test
    fun batterySwap_overviewWithinSaneRanges() {
        val overview = BatterySwapRepository.overview()
        assertTrue(overview.percent in 0..100)
        assertTrue(overview.rangeKm > 0)
        assertEquals(3, BatterySwapRepository.recentSwaps().size)
    }

    @Test
    fun wallet_transactionsHaveUniqueIdsAndPendingOnboardingFeeDebit() {
        val txs = WalletRepository.recentTransactions()
        assertEquals(txs.size, txs.map { it.id }.toSet().size)
        assertTrue(txs.any { it.isPending && it.type == TransactionType.DEBIT })
        assertTrue(
            txs.any {
                it.isPending &&
                    it.type == TransactionType.DEBIT &&
                    it.titleRes == R.string.wallet_tx_onboarding_fee &&
                    it.amountRes == R.string.wallet_tx_amount_2500
            }
        )
        assertTrue(
            txs.any {
                it.type == TransactionType.DEBIT &&
                    it.titleRes == R.string.wallet_tx_monthly_rental &&
                    it.amountRes == R.string.wallet_tx_amount_5900
            }
        )
    }

    @Test
    fun profile_menuContainsSettingsAndDocuments() {
        val approved = ProfileRepository.menuItems(kycApproved = true)
        assertTrue(approved.any { it.action == ProfileMenuAction.SETTINGS })
        assertTrue(approved.any { it.action == ProfileMenuAction.DOCUMENTS && it.showVerifiedBadge })
        assertEquals(5, approved.size)
        val pending = ProfileRepository.menuItems(kycApproved = false)
        assertTrue(pending.any { it.action == ProfileMenuAction.DOCUMENTS && !it.showVerifiedBadge })
    }

    @Test
    fun nearbyHubs_areHyderabadStations() {
        val ids = NearbyHubsRepository.stations().map { it.id }
        assertEquals(listOf("nagole", "kukatpally", "cherlapally"), ids)
    }
}
