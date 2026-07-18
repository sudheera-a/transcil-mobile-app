package com.example.transcilmobileapp.home

import com.example.transcilmobileapp.R

object WalletRepository {

    fun overview(): WalletOverview = WalletOverview(
        availableBalanceRes = R.string.wallet_balance_value,
        pendingRes = R.string.wallet_pending_value,
        thisMonthRes = R.string.wallet_this_month_value,
        todayEarningsRes = R.string.wallet_today_value,
        weeklyEarningsRes = R.string.wallet_weekly_value
    )

    fun recentTransactions(): List<WalletTransaction> = listOf(
        WalletTransaction(
            id = "1",
            titleRes = R.string.wallet_tx_daily_earnings,
            timeRes = R.string.wallet_tx_time_1,
            amountRes = R.string.wallet_tx_amount_1250,
            type = TransactionType.CREDIT
        ),
        WalletTransaction(
            id = "2",
            titleRes = R.string.wallet_tx_battery_fee,
            timeRes = R.string.wallet_tx_time_2,
            amountRes = R.string.wallet_tx_amount_50,
            type = TransactionType.DEBIT
        ),
        WalletTransaction(
            id = "3",
            titleRes = R.string.wallet_tx_daily_earnings,
            timeRes = R.string.wallet_tx_time_3,
            amountRes = R.string.wallet_tx_amount_1250,
            type = TransactionType.CREDIT
        ),
        WalletTransaction(
            id = "4",
            titleRes = R.string.wallet_tx_monthly_rental,
            timeRes = R.string.wallet_tx_time_4,
            amountRes = R.string.wallet_tx_amount_5900,
            type = TransactionType.DEBIT
        ),
        WalletTransaction(
            id = "5",
            titleRes = R.string.wallet_tx_onboarding_fee,
            timeRes = R.string.wallet_tx_time_5,
            amountRes = R.string.wallet_tx_amount_2500,
            type = TransactionType.DEBIT,
            isPending = true
        )
    )
}
