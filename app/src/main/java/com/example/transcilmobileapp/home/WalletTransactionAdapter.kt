package com.example.transcilmobileapp.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.databinding.ItemWalletTransactionBinding

class WalletTransactionAdapter : RecyclerView.Adapter<WalletTransactionAdapter.Holder>() {

    private val items = mutableListOf<WalletTransaction>()

    fun submit(list: List<WalletTransaction>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemWalletTransactionBinding.inflate(
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
        private val binding: ItemWalletTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WalletTransaction) {
            val context = binding.root.context
            binding.tvTxTitle.setText(item.titleRes)
            binding.tvTxTime.setText(item.timeRes)
            val amount = context.getString(item.amountRes)

            when (item.type) {
                TransactionType.CREDIT -> {
                    binding.ivTxIcon.setBackgroundResource(R.drawable.bg_circle_credit)
                    binding.ivTxIcon.setImageResource(R.drawable.ic_tx_credit)
                    binding.tvTxAmount.text = context.getString(R.string.wallet_amount_credit, amount)
                    binding.tvTxAmount.setTextColor(context.getColor(R.color.transaction_credit))
                }
                TransactionType.DEBIT -> {
                    binding.ivTxIcon.setBackgroundResource(R.drawable.bg_circle_debit)
                    binding.ivTxIcon.setImageResource(R.drawable.ic_tx_debit)
                    binding.tvTxAmount.text = context.getString(R.string.wallet_amount_debit, amount)
                    binding.tvTxAmount.setTextColor(context.getColor(R.color.transaction_debit))
                }
            }

            binding.tvTxPending.visibility = if (item.isPending) View.VISIBLE else View.GONE
        }
    }
}
