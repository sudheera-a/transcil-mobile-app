package com.example.transcilmobileapp.home

import androidx.annotation.StringRes

enum class TransactionType {
    CREDIT,
    DEBIT
}

data class WalletOverview(
    @param:StringRes val availableBalanceRes: Int,
    @param:StringRes val pendingRes: Int,
    @param:StringRes val thisMonthRes: Int,
    @param:StringRes val todayEarningsRes: Int,
    @param:StringRes val weeklyEarningsRes: Int
)

data class WalletTransaction(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val timeRes: Int,
    @param:StringRes val amountRes: Int,
    val type: TransactionType,
    val isPending: Boolean = false
)

enum class ProfileMenuAction {
    DOCUMENTS,
    SETTINGS,
    NOTIFICATIONS,
    HELP,
    PRIVACY
}

data class ProfileMenuItem(
    val action: ProfileMenuAction,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
    val iconRes: Int,
    val showVerifiedBadge: Boolean = false
)
