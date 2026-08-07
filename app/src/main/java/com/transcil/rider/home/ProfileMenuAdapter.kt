/**
 * RecyclerView adapter for profile menu rows; binds [ProfileMenuItem] to item layouts.
 * [submit] replaces the list and notifies the RecyclerView to redraw.
 */
package com.transcil.rider.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.transcil.rider.databinding.ItemProfileMenuBinding

class ProfileMenuAdapter(
    private val onClick: (ProfileMenuItem) -> Unit
) : RecyclerView.Adapter<ProfileMenuAdapter.Holder>() {

    private val items = mutableListOf<ProfileMenuItem>()

    // Adapter pattern: Fragment owns data flow; Adapter owns row inflation and binding.
    fun submit(list: List<ProfileMenuItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemProfileMenuBinding.inflate(
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

    // inner class Holder can read the adapter's onClick lambda directly.
    inner class Holder(
        private val binding: ItemProfileMenuBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProfileMenuItem) {
            binding.ivMenuIcon.setImageResource(item.iconRes)
            binding.tvMenuTitle.setText(item.titleRes)
            binding.tvMenuSubtitle.setText(item.subtitleRes)
            binding.tvMenuBadge.visibility =
                if (item.showVerifiedBadge) View.VISIBLE else View.GONE
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
