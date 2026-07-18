package com.example.transcilmobileapp.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.databinding.ItemRecentSwapBinding

class RecentSwapAdapter : RecyclerView.Adapter<RecentSwapAdapter.Holder>() {

    private val items = mutableListOf<RecentSwap>()

    fun submit(list: List<RecentSwap>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemRecentSwapBinding.inflate(
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

    class Holder(
        private val binding: ItemRecentSwapBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecentSwap) {
            val context = binding.root.context
            binding.tvStationName.setText(item.stationNameRes)
            binding.tvTimestamp.setText(item.timestampRes)
            binding.tvBefore.text = context.getString(
                R.string.battery_swap_percent,
                item.beforePercent
            )
            binding.tvAfter.text = context.getString(
                R.string.battery_swap_percent,
                item.afterPercent
            )
            binding.tvDuration.setText(item.durationRes)
        }
    }
}
