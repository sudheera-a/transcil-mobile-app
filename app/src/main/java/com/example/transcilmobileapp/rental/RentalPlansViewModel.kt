package com.example.transcilmobileapp.rental

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.transcilmobileapp.core.BaseViewModel
import com.example.transcilmobileapp.home.PlanType
import com.example.transcilmobileapp.home.VehicleModelId

class RentalPlansViewModel : BaseViewModel() {
    private val _modelId = MutableLiveData(VehicleModelId.ELLOD_ELITE)
    val modelId: LiveData<VehicleModelId> = _modelId

    private val _selected = MutableLiveData(PlanType.MONTHLY)
    val selected: LiveData<PlanType> = _selected

    fun bind(id: VehicleModelId) {
        _modelId.value = id
    }

    fun select(plan: PlanType) {
        _selected.value = plan
    }
}
