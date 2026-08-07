/**
 * ViewModel for VehiclesActivity — demo catalog with ALL/SCOOTER/BIKE filter and single selection.
 * Re-emits vehicle list on select() so Activity can refresh selection border styling.
 */
package com.transcil.rider.rental

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.transcil.rider.core.BaseViewModel
import com.transcil.rider.home.RentalCatalog
import com.transcil.rider.home.VehicleModelId

/** `data class`: one vehicle card row — UI-ready fields including availability and hub distance. */
data class VehicleUi(
    val id: VehicleModelId,
    val title: String,
    @param:DrawableRes val imageRes: Int,
    val batteryPercent: Int,
    val rangeKm: Int,
    val rating: String,
    val hub: String,
    val available: Boolean,
    val statusLabel: String,
    val kind: String,
)

class VehiclesViewModel : BaseViewModel() {
    // `private val`: immutable demo seed data — filter creates new lists, never mutates this.
    private val all = listOf(
        VehicleUi(
            VehicleModelId.ELLOD_ELITE,
            "E-Scooter · Ellod Elite",
            RentalCatalog.model(VehicleModelId.ELLOD_ELITE).imageRes,
            92, 78, "4.8", "Koramangala Hub · 1.2 km", true, "", "SCOOTER",
        ),
        VehicleUi(
            VehicleModelId.ELACIL_2_5,
            "E-Scooter · Elacil 2.5",
            RentalCatalog.model(VehicleModelId.ELACIL_2_5).imageRes,
            54, 41, "4.6", "Indiranagar · 3.8 km", true, "", "SCOOTER",
        ),
        VehicleUi(
            VehicleModelId.ELLOD_ELITE,
            "E-Bike · Hero Vida",
            RentalCatalog.model(VehicleModelId.ELLOD_ELITE).imageRes,
            0, 0, "4.5", "Indiranagar", false, "Back on 26 Jul · Indiranagar", "BIKE",
        ),
    )

    private val _filter = MutableLiveData("ALL")
    val filter: LiveData<String> = _filter

    private val _vehicles = MutableLiveData(all)
    val vehicles: LiveData<List<VehicleUi>> = _vehicles

    private val _selectedId = MutableLiveData<VehicleModelId?>(VehicleModelId.ELLOD_ELITE)
    val selectedId: LiveData<VehicleModelId?> = _selectedId

    fun setFilter(filter: String) {
        _filter.value = filter
        _vehicles.value = when (filter) {
            "SCOOTER", "BIKE" -> all.filter { it.kind == filter }
            else -> all
        }
    }

    fun select(id: VehicleModelId) {
        _selectedId.value = id
        // refresh list selection borders via re-emit
        _vehicles.value = _vehicles.value
    }
}
