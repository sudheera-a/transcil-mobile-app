package com.example.transcilmobileapp.home

import androidx.annotation.StringRes

enum class StationStatus {
    ACTIVE,
    PENDING
}

data class SwapStation(
    val id: String,
    @param:StringRes val nameRes: Int,
    val status: StationStatus,
    val distanceKm: String,
    val available: Int,
    val capacity: Int
)

data class BatteryOverview(
    val percent: Int,
    val rangeKm: Int,
    @param:StringRes val lastSwapRes: Int,
    @param:StringRes val totalSwapsRes: Int
)

data class RecentSwap(
    val id: String,
    @param:StringRes val stationNameRes: Int,
    @param:StringRes val timestampRes: Int,
    val beforePercent: Int,
    val afterPercent: Int,
    @param:StringRes val durationRes: Int
)
