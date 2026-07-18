package com.example.transcilmobileapp.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.kyc.KycProgressRepository

enum class HomeQuickAction {
    BATTERY_SWAP,
    NAVIGATE,
    NEARBY_HUBS,
    EXTEND_RENTAL
}

enum class HomeNavTab {
    HOME,
    MAP,
    BATTERY,
    WALLET,
    PROFILE
}

class HomeDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _kycStatus = MutableLiveData(KycStatus.PENDING)
    val kycStatus: LiveData<KycStatus> = _kycStatus

    private val _riderName = MutableLiveData("")
    val riderName: LiveData<String> = _riderName

    private val _transcilId = MutableLiveData("")
    val transcilId: LiveData<String> = _transcilId

    private val _showStubMessage = MutableLiveData<Int?>()
    val showStubMessage: LiveData<Int?> = _showStubMessage

    private val _navigateTab = MutableLiveData<HomeNavTab?>()
    val navigateTab: LiveData<HomeNavTab?> = _navigateTab

    fun bind(status: KycStatus) {
        _kycStatus.value = status
        val draftName = KycProgressRepository.personalDraft().fullName.trim()
        _riderName.value = draftName.ifBlank {
            getApplication<Application>().getString(R.string.home_default_rider)
        }
        _transcilId.value = getApplication<Application>().getString(
            R.string.home_transcil_id,
            getApplication<Application>().getString(R.string.home_transcil_id_stub)
        )
    }

    fun onQuickAction(action: HomeQuickAction) {
        when (action) {
            HomeQuickAction.BATTERY_SWAP -> _navigateTab.value = HomeNavTab.BATTERY
            HomeQuickAction.NAVIGATE, HomeQuickAction.NEARBY_HUBS -> {
                _navigateTab.value = HomeNavTab.MAP
            }
            HomeQuickAction.EXTEND_RENTAL -> {
                _showStubMessage.value = R.string.home_action_extend_stub
            }
        }
    }

    fun onActionClicked() {
        _showStubMessage.value = R.string.home_action_stub
    }

    fun clearStubMessage() {
        _showStubMessage.value = null
    }

    fun clearNavigateTab() {
        _navigateTab.value = null
    }
}
