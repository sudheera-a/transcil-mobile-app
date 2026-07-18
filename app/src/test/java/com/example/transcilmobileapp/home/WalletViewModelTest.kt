package com.example.transcilmobileapp.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.transcilmobileapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WalletViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun overviewAndTransactions_loadFromRepository() {
        val vm = WalletViewModel()
        assertEquals(R.string.wallet_balance_value, vm.overview.value?.availableBalanceRes)
        val txs = vm.transactions.value.orEmpty()
        assertTrue(txs.size >= 5)
        assertTrue(txs.any { it.isPending })
        assertTrue(txs.any { it.type == TransactionType.CREDIT })
        assertTrue(txs.any { it.type == TransactionType.DEBIT })
    }

    @Test
    fun onWithdraw_andViewAll_emitStubToasts_thenClear() {
        val vm = WalletViewModel()
        vm.onWithdraw()
        assertEquals(R.string.wallet_withdraw_stub, vm.toastMessage.value)
        vm.clearToast()
        assertNull(vm.toastMessage.value)

        vm.onViewAll()
        assertEquals(R.string.wallet_view_all_stub, vm.toastMessage.value)
        vm.clearToast()
        assertNull(vm.toastMessage.value)
    }
}
