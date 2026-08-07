/**
 * Profile tab: rider details, KYC badge, and a menu list of settings/help links.
 * Routes navigation via [ProfileViewModel.navEvent] and Jetpack Navigation actions.
 */
package com.transcil.rider.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import com.transcil.rider.R
import com.transcil.rider.auth.AuthSession
import com.transcil.rider.core.KycNavigator
import com.transcil.rider.core.KycStatus
import com.transcil.rider.databinding.FragmentProfileBinding
import com.transcil.rider.kyc.KycProgressActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    // RecyclerView.Adapter renders the scrollable profile menu rows.
    private lateinit var adapter: ProfileMenuAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val status = requireActivity().intent.getStringExtra(KycNavigator.EXTRA_KYC_STATUS)
            ?.let { runCatching { KycStatus.valueOf(it) }.getOrNull() }
            ?: KycStatus.PENDING
        viewModel.bind(status)

        adapter = ProfileMenuAdapter { item -> viewModel.onMenuClicked(item.action) }
        binding.rvMenu.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMenu.adapter = adapter

        binding.btnBack.setOnClickListener {
            (activity as? HomeDashboardActivity)?.navigateToTab(HomeNavTab.HOME)
        }
        binding.btnEdit.setOnClickListener { viewModel.onEdit() }
        binding.btnLogout.setOnClickListener { viewModel.onLogout() }

        viewModel.displayName.observe(viewLifecycleOwner) { binding.tvProfileName.text = it }
        viewModel.riderId.observe(viewLifecycleOwner) { binding.tvRiderId.text = it }
        viewModel.phone.observe(viewLifecycleOwner) { binding.tvPhone.text = it }
        viewModel.email.observe(viewLifecycleOwner) { binding.tvEmail.text = it }
        viewModel.location.observe(viewLifecycleOwner) { binding.tvLocation.text = it }
        viewModel.kycStatus.observe(viewLifecycleOwner, ::renderKycBadge)
        viewModel.menuItems.observe(viewLifecycleOwner) { adapter.submit(it.orEmpty()) }
        viewModel.toastMessage.observe(viewLifecycleOwner) { resId ->
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
        // sealed class branches give exhaustive when — each nav outcome is a typed variant.
        viewModel.navEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                ProfileNavEvent.OpenSettings -> {
                    findNavController().navigate(
                        R.id.action_profile_to_settings,
                        null,
                        navOptions { launchSingleTop = true }
                    )
                    viewModel.clearNavEvent()
                }
                ProfileNavEvent.OpenDocuments -> {
                    // Keep Home shell alive — do not use KycFlowNavigator.openProgress (it finishes).
                    // Browse-only: already-approved riders must not bounce to KycApprovedActivity.
                    startActivity(
                        Intent(requireContext(), KycProgressActivity::class.java)
                            .putExtra(KycProgressActivity.EXTRA_BROWSE_ONLY, true),
                    )
                    viewModel.clearNavEvent()
                }
                is ProfileNavEvent.OpenContent -> {
                    if (event.page == ContentPage.HELP) {
                        findNavController().navigate(
                            R.id.action_profile_to_help,
                            null,
                            navOptions { launchSingleTop = true },
                        )
                    } else {
                        findNavController().navigate(
                            R.id.action_profile_to_api_content,
                            bundleOf(ApiContentFragment.ARG_PAGE to event.page.name),
                            navOptions { launchSingleTop = true },
                        )
                    }
                    viewModel.clearNavEvent()
                }
                is ProfileNavEvent.ShowStub -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.profile_menu_stub, getString(event.titleRes)),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.clearNavEvent()
                }
                ProfileNavEvent.SignedOut -> {
                    AuthSession.openSignedOut(requireContext())
                    viewModel.clearNavEvent()
                }
                null -> Unit
            }
        }
    }

    private fun renderKycBadge(status: KycStatus?) {
        val approved = status == KycStatus.APPROVED
        if (approved) {
            binding.tvKycBadge.text = getString(R.string.profile_verified)
            binding.tvKycBadge.setBackgroundResource(R.drawable.bg_badge_approved)
            binding.tvKycBadge.setTextColor(requireContext().getColor(R.color.status_approved))
        } else {
            binding.tvKycBadge.text = getString(R.string.profile_pending)
            binding.tvKycBadge.setBackgroundResource(R.drawable.bg_badge_pending)
            binding.tvKycBadge.setTextColor(requireContext().getColor(R.color.status_pending))
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh if user edited KYC drafts and came back.
        val status = requireActivity().intent.getStringExtra(KycNavigator.EXTRA_KYC_STATUS)
            ?.let { runCatching { KycStatus.valueOf(it) }.getOrNull() }
            ?: KycStatus.PENDING
        viewModel.bind(status)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
