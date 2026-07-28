package com.example.transcilmobileapp.rental

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.databinding.ActivityVehiclesBinding
import com.example.transcilmobileapp.databinding.ItemVehicleSelectCardBinding
import com.example.transcilmobileapp.home.VehicleModelId

class VehiclesActivity : BaseActivity<ActivityVehiclesBinding>(ActivityVehiclesBinding::inflate) {

    private val viewModel: VehiclesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.ivBack.setOnClickListener { finish() }
        listOf(binding.chipAll, binding.chipScooter, binding.chipBike).forEach { chip ->
            chip.setOnClickListener { viewModel.setFilter(chip.tag as String) }
        }
        viewModel.filter.observe(this) { renderFilter(it) }
        viewModel.vehicles.observe(this) { renderList(it) }
        viewModel.selectedId.observe(this) { id ->
            binding.btnSelect.isEnabled = id != null
        }
        binding.btnSelect.setOnClickListener {
            val id = viewModel.selectedId.value ?: return@setOnClickListener
            startActivity(RentalPlansActivity.createIntent(this, id.name))
        }
    }

    private fun renderFilter(filter: String) {
        fun style(view: View, selected: Boolean) {
            view.setBackgroundResource(
                if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_default,
            )
        }
        style(binding.chipAll, filter == "ALL")
        style(binding.chipScooter, filter == "SCOOTER")
        style(binding.chipBike, filter == "BIKE")
    }

    private fun renderList(items: List<VehicleUi>) {
        val row = binding.vehicleList
        row.removeAllViews()
        val inflater = LayoutInflater.from(this)
        items.forEach { item ->
            val card = ItemVehicleSelectCardBinding.inflate(inflater, row, false)
            card.ivThumb.setImageResource(item.imageRes)
            card.tvTitle.text = item.title
            card.tvMeta.text = getString(
                R.string.vehicle_card_meta,
                item.batteryPercent,
                item.rangeKm,
                item.rating,
            )
            card.tvHub.text = item.hub
            card.tvStatus.isVisible = !item.available
            card.tvStatus.text = item.statusLabel
            card.root.alpha = if (item.available) 1f else 0.55f
            card.root.setOnClickListener {
                if (item.available) viewModel.select(item.id) else {
                    Toast.makeText(this, item.statusLabel, Toast.LENGTH_SHORT).show()
                }
            }
            val selected = viewModel.selectedId.value == item.id
            card.root.setBackgroundResource(
                if (selected) R.drawable.bg_card_selected else R.drawable.bg_card_default,
            )
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_md) }
            card.root.layoutParams = lp
            row.addView(card.root)
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, VehiclesActivity::class.java)
    }
}
