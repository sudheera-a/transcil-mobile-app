/**
 * ViewModel for RentalPlansActivity — holds selected vehicle model and chosen plan type.
 * Defaults to MONTHLY plan; Activity reads [selected] to style cards and sticky footer.
 */
package com.transcil.rider.rental

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.transcil.rider.core.BaseViewModel
import com.transcil.rider.home.PlanType
import com.transcil.rider.home.VehicleModelId

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
