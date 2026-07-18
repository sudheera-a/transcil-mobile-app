package com.example.transcilmobileapp.home

import com.example.transcilmobileapp.R

/**
 * Stub battery + history feed. Replace with API-backed repository later.
 */
object BatterySwapRepository {

    fun overview(): BatteryOverview = BatteryOverview(
        percent = 72,
        rangeKm = 58,
        lastSwapRes = R.string.battery_swap_last_swap_value,
        totalSwapsRes = R.string.battery_swap_total_swaps_value
    )

    fun recentSwaps(): List<RecentSwap> = listOf(
        RecentSwap(
            id = "1",
            stationNameRes = R.string.nearby_hubs_station_nagole,
            timestampRes = R.string.battery_swap_timestamp_1,
            beforePercent = 18,
            afterPercent = 95,
            durationRes = R.string.battery_swap_duration_1
        ),
        RecentSwap(
            id = "2",
            stationNameRes = R.string.nearby_hubs_station_kukatpally,
            timestampRes = R.string.battery_swap_timestamp_2,
            beforePercent = 22,
            afterPercent = 98,
            durationRes = R.string.battery_swap_duration_2
        ),
        RecentSwap(
            id = "3",
            stationNameRes = R.string.nearby_hubs_station_cherlapally,
            timestampRes = R.string.battery_swap_timestamp_3,
            beforePercent = 15,
            afterPercent = 92,
            durationRes = R.string.battery_swap_duration_3
        )
    )
}
