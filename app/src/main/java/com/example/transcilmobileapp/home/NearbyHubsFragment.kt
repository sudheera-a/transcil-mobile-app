package com.example.transcilmobileapp.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.databinding.FragmentNearbyHubsBinding

class NearbyHubsFragment : Fragment() {

    private var _binding: FragmentNearbyHubsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NearbyHubsViewModel by viewModels()
    private lateinit var adapter: SwapStationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNearbyHubsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SwapStationAdapter { station ->
            viewModel.onNavigateClicked(getString(station.nameRes))
        }
        binding.rvStations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStations.adapter = adapter

        binding.btnBack.setOnClickListener {
            (activity as? HomeDashboardActivity)?.navigateToTab(HomeNavTab.HOME)
                ?: findNavController().navigateUp()
        }
        binding.btnRecenter.setOnClickListener {
            Toast.makeText(requireContext(), R.string.home_action_stub, Toast.LENGTH_SHORT).show()
        }

        viewModel.stations.observe(viewLifecycleOwner) { adapter.submit(it.orEmpty()) }
        viewModel.navigateMessage.observe(viewLifecycleOwner) { resId ->
            val name = viewModel.navigateStationName.value
            if (resId != null && name != null) {
                Toast.makeText(
                    requireContext(),
                    getString(resId, name),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.clearNavigateMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
