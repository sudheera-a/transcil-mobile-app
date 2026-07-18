package com.example.transcilmobileapp.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.transcilmobileapp.R

class NearbyHubsViewModel : ViewModel() {

    private val _stations = MutableLiveData(NearbyHubsRepository.stations())
    val stations: LiveData<List<SwapStation>> = _stations

    private val _navigateMessage = MutableLiveData<Int?>()
    val navigateMessage: LiveData<Int?> = _navigateMessage

    private val _navigateStationName = MutableLiveData<String?>()
    val navigateStationName: LiveData<String?> = _navigateStationName

    fun onNavigateClicked(stationName: String) {
        _navigateStationName.value = stationName
        _navigateMessage.value = R.string.nearby_hubs_navigate_stub
    }

    fun clearNavigateMessage() {
        _navigateMessage.value = null
        _navigateStationName.value = null
    }
}
