package com.example.transcilmobileapp.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.transcilmobileapp.databinding.FragmentWalletBinding

class WalletFragment : Fragment() {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WalletViewModel by viewModels()
    private val adapter = WalletTransactionAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter

        binding.btnBack.setOnClickListener {
            (activity as? HomeDashboardActivity)?.navigateToTab(HomeNavTab.HOME)
        }
        binding.btnWithdraw.setOnClickListener { viewModel.onWithdraw() }
        binding.btnViewAll.setOnClickListener { viewModel.onViewAll() }

        viewModel.overview.observe(viewLifecycleOwner) { overview ->
            if (overview == null) return@observe
            binding.tvAvailableBalance.setText(overview.availableBalanceRes)
            binding.tvPending.setText(overview.pendingRes)
            binding.tvThisMonth.setText(overview.thisMonthRes)
            binding.tvTodayEarnings.setText(overview.todayEarningsRes)
            binding.tvWeeklyEarnings.setText(overview.weeklyEarningsRes)
        }
        viewModel.transactions.observe(viewLifecycleOwner) { adapter.submit(it.orEmpty()) }
        viewModel.toastMessage.observe(viewLifecycleOwner) { resId ->
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
