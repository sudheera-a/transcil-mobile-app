package com.transcil.rider.bluetooth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.transcil.rider.databinding.ItemBluetoothDeviceBinding

class BluetoothDeviceAdapter(
    private val onClick: (BluetoothDeviceItem) -> Unit,
) : RecyclerView.Adapter<BluetoothDeviceAdapter.Holder>() {

    private val items = mutableListOf<BluetoothDeviceItem>()

    fun submit(list: List<BluetoothDeviceItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemBluetoothDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class Holder(
        private val binding: ItemBluetoothDeviceBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BluetoothDeviceItem) {
            binding.tvName.text = item.name
            binding.tvAddress.text = item.address
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
