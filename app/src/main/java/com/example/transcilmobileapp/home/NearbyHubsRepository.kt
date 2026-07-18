package com.example.transcilmobileapp.home

import com.example.transcilmobileapp.R

/**
 * Stub station feed. Swap this implementation for a network/API source later
 * without changing the Fragment or ViewModel contracts.
 */
object NearbyHubsRepository {

    fun stations(): List<SwapStation> = listOf(
        SwapStation(
            id = "nagole",
            nameRes = R.string.nearby_hubs_station_nagole,
            status = StationStatus.ACTIVE,
            distanceKm = "1.2",
            available = 8,
            capacity = 12
        ),
        SwapStation(
            id = "kukatpally",
            nameRes = R.string.nearby_hubs_station_kukatpally,
            status = StationStatus.ACTIVE,
            distanceKm = "2.8",
            available = 5,
            capacity = 10
        ),
        SwapStation(
            id = "cherlapally",
            nameRes = R.string.nearby_hubs_station_cherlapally,
            status = StationStatus.PENDING,
            distanceKm = "3.5",
            available = 0,
            capacity = 8
        )
    )
}
