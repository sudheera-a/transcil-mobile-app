package com.example.transcilmobileapp.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.databinding.ItemSwapStationBinding

class SwapStationAdapter(
    private val onNavigate: (SwapStation) -> Unit
) : RecyclerView.Adapter<SwapStationAdapter.Holder>() {

    private val items = mutableListOf<SwapStation>()

    fun submit(list: List<SwapStation>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSwapStationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(
        private val binding: ItemSwapStationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(station: SwapStation) {
            val context = binding.root.context
            binding.tvStationName.setText(station.nameRes)
            binding.tvDistance.text = context.getString(
                R.string.nearby_hubs_distance,
                station.distanceKm
            )
            binding.tvAvailability.text = context.getString(
                R.string.nearby_hubs_availability,
                station.available,
                station.capacity
            )

            when (station.status) {
                StationStatus.ACTIVE -> {
                    binding.tvStatus.setText(R.string.nearby_hubs_status_active)
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_approved)
                    binding.tvStatus.setTextColor(context.getColor(R.color.status_approved))
                }
                StationStatus.PENDING -> {
                    binding.tvStatus.setText(R.string.nearby_hubs_status_pending)
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
                    binding.tvStatus.setTextColor(context.getColor(R.color.status_pending))
                }
            }

            binding.btnNavigate.setOnClickListener { onNavigate(station) }
        }
    }
}
