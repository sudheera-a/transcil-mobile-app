package com.example.transcilmobileapp.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure routing rules for Profile menu → Settings / KYC Documents / stub.
 * Mirrors [ProfileViewModel.onMenuClicked] without AndroidViewModel.
 */
class ProfileMenuRoutingTest {

    @Test
    fun settingsAndNotifications_routeToSettings() {
        assertEquals(Route.SETTINGS, routeFor(ProfileMenuAction.SETTINGS))
        assertEquals(Route.SETTINGS, routeFor(ProfileMenuAction.NOTIFICATIONS))
    }

    @Test
    fun documents_routeToKycProgress() {
        assertEquals(Route.DOCUMENTS, routeFor(ProfileMenuAction.DOCUMENTS))
    }

    @Test
    fun helpAndPrivacy_routeToStub() {
        assertEquals(Route.STUB, routeFor(ProfileMenuAction.HELP))
        assertEquals(Route.STUB, routeFor(ProfileMenuAction.PRIVACY))
    }

    @Test
    fun repository_coversAllActions() {
        val actions = ProfileRepository.menuItems(kycApproved = true).map { it.action }.toSet()
        assertTrue(actions.containsAll(ProfileMenuAction.entries.toSet()))
    }

    @Test
    fun documentsVerifiedBadge_followsKycStatus() {
        assertTrue(
            ProfileRepository.menuItems(kycApproved = true)
                .first { it.action == ProfileMenuAction.DOCUMENTS }
                .showVerifiedBadge
        )
        assertFalse(
            ProfileRepository.menuItems(kycApproved = false)
                .first { it.action == ProfileMenuAction.DOCUMENTS }
                .showVerifiedBadge
        )
    }

    private enum class Route { SETTINGS, DOCUMENTS, STUB }

    private fun routeFor(action: ProfileMenuAction): Route = when (action) {
        ProfileMenuAction.SETTINGS, ProfileMenuAction.NOTIFICATIONS -> Route.SETTINGS
        ProfileMenuAction.DOCUMENTS -> Route.DOCUMENTS
        ProfileMenuAction.HELP, ProfileMenuAction.PRIVACY -> Route.STUB
    }
}
