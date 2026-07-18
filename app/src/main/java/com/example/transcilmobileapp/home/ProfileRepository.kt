package com.example.transcilmobileapp.home

import com.example.transcilmobileapp.R

object ProfileRepository {

    fun menuItems(kycApproved: Boolean): List<ProfileMenuItem> = listOf(
        ProfileMenuItem(
            action = ProfileMenuAction.DOCUMENTS,
            titleRes = R.string.profile_menu_documents,
            subtitleRes = R.string.profile_menu_documents_sub,
            iconRes = R.drawable.ic_document,
            showVerifiedBadge = kycApproved
        ),
        ProfileMenuItem(
            action = ProfileMenuAction.SETTINGS,
            titleRes = R.string.profile_menu_settings,
            subtitleRes = R.string.profile_menu_settings_sub,
            iconRes = R.drawable.ic_settings
        ),
        ProfileMenuItem(
            action = ProfileMenuAction.NOTIFICATIONS,
            titleRes = R.string.profile_menu_notifications,
            subtitleRes = R.string.profile_menu_notifications_sub,
            iconRes = R.drawable.ic_notifications
        ),
        ProfileMenuItem(
            action = ProfileMenuAction.HELP,
            titleRes = R.string.profile_menu_help,
            subtitleRes = R.string.profile_menu_help_sub,
            iconRes = R.drawable.ic_help
        ),
        ProfileMenuItem(
            action = ProfileMenuAction.PRIVACY,
            titleRes = R.string.profile_menu_privacy,
            subtitleRes = R.string.profile_menu_privacy_sub,
            iconRes = R.drawable.ic_shield
        )
    )
}
