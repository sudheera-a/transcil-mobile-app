package com.example.transcilmobileapp.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.transcilmobileapp.R

sealed class BatterySwapEvent {
    data object ScanQr : BatterySwapEvent()
    data object FindStation : BatterySwapEvent()
}

class BatterySwapViewModel : ViewModel() {

    private val _overview = MutableLiveData(BatterySwapRepository.overview())
    val overview: LiveData<BatteryOverview> = _overview

    private val _recentSwaps = MutableLiveData(BatterySwapRepository.recentSwaps())
    val recentSwaps: LiveData<List<RecentSwap>> = _recentSwaps

    private val _event = MutableLiveData<BatterySwapEvent?>()
    val event: LiveData<BatterySwapEvent?> = _event

    private val _toastMessage = MutableLiveData<Int?>()
    val toastMessage: LiveData<Int?> = _toastMessage

    fun onScanQr() {
        _toastMessage.value = R.string.battery_swap_scan_stub
        _event.value = BatterySwapEvent.ScanQr
    }

    fun onFindStation() {
        _event.value = BatterySwapEvent.FindStation
    }

    fun clearEvent() {
        _event.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
