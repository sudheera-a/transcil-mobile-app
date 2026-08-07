/**
 * Battery swap tab: current charge overview, QR/find-station actions, recent swap history.
 */
package com.transcil.rider.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.transcil.rider.R
import com.transcil.rider.databinding.FragmentBatterySwapBinding

class BatterySwapFragment : Fragment() {

    private var _binding: FragmentBatterySwapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BatterySwapViewModel by viewModels()
    // RecyclerView.Adapter for the "recent swaps" list under the battery overview card.
    private val adapter = RecentSwapAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatterySwapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvRecentSwaps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentSwaps.adapter = adapter

        binding.btnBack.setOnClickListener {
            (activity as? HomeDashboardActivity)?.navigateToTab(HomeNavTab.HOME)
                ?: findNavController().navigateUp()
        }
        binding.btnScanQr.setOnClickListener { viewModel.onScanQr() }
        binding.btnFindStation.setOnClickListener { viewModel.onFindStation() }

        viewModel.overview.observe(viewLifecycleOwner) { overview ->
            if (overview == null) return@observe
            binding.tvBatteryPercent.text = getString(
                R.string.battery_swap_percent,
                overview.percent
            )
            binding.tvBatteryRange.text = getString(
                R.string.battery_swap_range,
                overview.rangeKm
            )
            binding.tvLastSwap.setText(overview.lastSwapRes)
            binding.tvTotalSwaps.setText(overview.totalSwapsRes)
        }
        viewModel.recentSwaps.observe(viewLifecycleOwner) { adapter.submit(it.orEmpty()) }
        viewModel.toastMessage.observe(viewLifecycleOwner) { resId ->
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                BatterySwapEvent.FindStation -> {
                    // Activity-level tab switch when hosted inside HomeDashboardActivity.
                    (activity as? HomeDashboardActivity)?.navigateToTab(HomeNavTab.MAP)
                    viewModel.clearEvent()
                }
                BatterySwapEvent.ScanQr -> viewModel.clearEvent()
                null -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
