package com.example.transcilmobileapp.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.KycNavigator
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.databinding.FragmentHomeDashboardBinding
import com.example.transcilmobileapp.databinding.ItemHomeVehicleCardBinding
import com.example.transcilmobileapp.payment.PaymentActivity
import com.example.transcilmobileapp.rental.VehiclesActivity

class HomeDashboardFragment : Fragment() {

    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeDashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val status = requireActivity().intent.getStringExtra(KycNavigator.EXTRA_KYC_STATUS)
            ?.let { runCatching { KycStatus.valueOf(it) }.getOrNull() }
            ?: KycStatus.PENDING
        viewModel.bind(status)

        setupVehicleCarousel()
        bindActions()
        viewModel.kycStatus.observe(viewLifecycleOwner, ::renderStatus)
        viewModel.riderName.observe(viewLifecycleOwner) { name ->
            binding.tvGreeting.text = name
            binding.tvProfileName.text = name
        }
        viewModel.transcilId.observe(viewLifecycleOwner) { id ->
            binding.tvTranscilId.text = id
        }
        viewModel.showStubMessage.observe(viewLifecycleOwner) { resId ->
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
                viewModel.clearStubMessage()
            }
        }
        viewModel.navigateTab.observe(viewLifecycleOwner) { tab ->
            if (tab != null) {
                (activity as? HomeDashboardActivity)?.navigateToTab(tab)
                viewModel.clearNavigateTab()
            }
        }
    }

    private fun setupVehicleCarousel() {
        val row = binding.vehicleCarouselRow
        row.removeAllViews()
        val vehicles = RentalCatalog.models()
        val cardWidth = resources.getDimensionPixelSize(R.dimen.home_vehicle_card_width)
        val gap = resources.getDimensionPixelSize(R.dimen.home_vehicle_card_gap)
        val inflater = LayoutInflater.from(requireContext())

        vehicles.forEachIndexed { index, spec ->
            val cardBinding = ItemHomeVehicleCardBinding.inflate(inflater, row, false)
            cardBinding.tvVehicleModel.setText(spec.displayNameRes)
            cardBinding.tvVehicleType.setText(R.string.home_vehicle_type)
            cardBinding.ivVehicleImage.setImageResource(spec.imageRes)

            val lp = ViewGroup.MarginLayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (index < vehicles.lastIndex) {
                lp.marginEnd = gap
            }
            cardBinding.root.layoutParams = lp

            val modelName = getString(spec.displayNameRes)
            cardBinding.btnVehicleDetails.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.home_vehicle_details) + " · " + modelName,
                    Toast.LENGTH_SHORT
                ).show()
            }
            cardBinding.btnVehicleSelect.setOnClickListener {
                startActivity(VehiclesActivity.createIntent(requireContext()))
            }
            row.addView(cardBinding.root)
        }
    }

    private fun renderStatus(status: KycStatus?) {
        val approved = status == KycStatus.APPROVED
        binding.cardProfilePending.visibility = if (approved) View.GONE else View.VISIBLE
        binding.cardAvailableVehicle.visibility = if (approved) View.GONE else View.VISIBLE
        binding.cardActiveVehicle.visibility = if (approved) View.VISIBLE else View.GONE
        binding.bannerRentalDue.visibility = if (approved) View.VISIBLE else View.GONE

        if (approved) {
            binding.tvKycBadge.text = getString(R.string.home_status_approved)
            binding.tvKycBadge.setBackgroundResource(R.drawable.bg_badge_approved)
            binding.tvKycBadge.setTextColor(requireContext().getColor(R.color.status_approved))
        } else {
            binding.tvKycBadge.text = getString(R.string.home_status_pending)
            binding.tvKycBadge.setBackgroundResource(R.drawable.bg_badge_pending)
            binding.tvKycBadge.setTextColor(requireContext().getColor(R.color.status_pending))
        }
    }

    private fun bindActions() {
        binding.btnNotifications.setOnClickListener { viewModel.onActionClicked() }

        binding.btnSwapBattery.setOnClickListener {
            viewModel.onQuickAction(HomeQuickAction.BATTERY_SWAP)
        }
        binding.btnNavigateHero.setOnClickListener {
            viewModel.onQuickAction(HomeQuickAction.NAVIGATE)
        }
        binding.btnHubsHero.setOnClickListener {
            viewModel.onQuickAction(HomeQuickAction.NEARBY_HUBS)
        }
        binding.btnBrowseVehicles.setOnClickListener {
            startActivity(VehiclesActivity.createIntent(requireContext()))
        }
        binding.btnPayNow.setOnClickListener {
            startActivity(
                PaymentActivity.createIntent(
                    requireContext(),
                    RentalCatalog.defaultActiveModel().id.name,
                    PlanType.MONTHLY.name,
                ),
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
