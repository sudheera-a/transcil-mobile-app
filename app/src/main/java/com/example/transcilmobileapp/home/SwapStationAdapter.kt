package com.example.transcilmobileapp.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.databinding.ItemSwapStationBinding

class SwapStationAdapter(
    private val onNavigate: (SwapStation) -> Unit,
) : RecyclerView.Adapter<SwapStationAdapter.Holder>() {

    private val items = mutableListOf<SwapStation>()

    fun submit(list: List<SwapStation>) {
        items.clear()
        items.addAll(list.sortedBy { it.distanceKm.toFloatOrNull() ?: Float.MAX_VALUE })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSwapStationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(
        private val binding: ItemSwapStationBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(station: SwapStation) {
            val context = binding.root.context
            val soldOut = station.available <= 0
            binding.root.alpha = if (soldOut) 0.55f else 1f
            binding.tvStationName.setText(station.nameRes)
            binding.tvDistance.text = context.getString(
                R.string.nearby_hubs_distance_eta,
                station.distanceKm,
                etaMinutes(station.distanceKm),
            )
            binding.tvAvailability.text = if (soldOut) {
                context.getString(R.string.hubs_no_batteries)
            } else {
                context.getString(
                    R.string.nearby_hubs_availability_ready,
                    station.available,
                    station.capacity,
                )
            }

            if (soldOut) {
                binding.tvStatus.setText(R.string.hubs_no_batteries)
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
                binding.tvStatus.setTextColor(context.getColor(R.color.status_pending))
            } else {
                binding.tvStatus.setText(R.string.hubs_open)
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_approved)
                binding.tvStatus.setTextColor(context.getColor(R.color.status_approved))
            }

            binding.btnNavigate.setOnClickListener { onNavigate(station) }
        }

        private fun etaMinutes(distanceKm: String): Int {
            val km = distanceKm.toFloatOrNull() ?: 1f
            return (km * 5).toInt().coerceAtLeast(3)
        }
    }
}
