/**
 * Settings sub-screen (stacked on Profile): notification toggles, theme, and account actions.
 * Back navigation uses Jetpack Navigation; bottom nav is hidden while this Fragment is shown.
 */
package com.transcil.rider.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.transcil.rider.R
import com.transcil.rider.auth.AuthSession
import com.transcil.rider.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val goBack = { findNavController().navigateUp() }
        // OnBackPressedCallback intercepts system back while this Fragment is visible.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goBack()
                }
            },
        )
        binding.btnBack.setOnClickListener { goBack() }

        binding.switchRentalAlerts.setOnCheckedChangeListener { _, checked ->
            viewModel.onRentalAlerts(checked)
        }
        binding.switchBatteryAlerts.setOnCheckedChangeListener { _, checked ->
            viewModel.onBatteryAlerts(checked)
        }
        binding.switchOfferAlerts.setOnCheckedChangeListener { _, checked ->
            viewModel.onOfferAlerts(checked)
        }
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onNotificationsToggled(isChecked)
        }
        binding.rowLanguage.setOnClickListener { viewModel.onLanguage() }
        binding.rowBluetooth.setOnClickListener {
            findNavController().navigate(
                R.id.action_settings_to_bluetooth,
                null,
                navOptions { launchSingleTop = true },
            )
        }
        binding.rowChangePassword.setOnClickListener { viewModel.onChangePassword() }
        binding.rowHelpCenter.setOnClickListener { viewModel.onHelpCenter() }
        binding.rowTerms.setOnClickListener { viewModel.onTerms() }
        binding.btnLogout.setOnClickListener { viewModel.onLogout() }

        viewModel.notificationsEnabled.observe(viewLifecycleOwner) { enabled ->
            if (binding.switchNotifications.isChecked != enabled) {
                binding.switchNotifications.isChecked = enabled == true
            }
        }
        viewModel.toastMessage.observe(viewLifecycleOwner) { resId ->
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
        viewModel.navEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is SettingsNavEvent.OpenContent -> {
                    if (event.page == ContentPage.HELP) {
                        findNavController().navigate(
                            R.id.action_settings_to_help,
                            null,
                            navOptions { launchSingleTop = true },
                        )
                    } else {
                        findNavController().navigate(
                            R.id.action_settings_to_api_content,
                            bundleOf(ApiContentFragment.ARG_PAGE to event.page.name),
                            navOptions { launchSingleTop = true },
                        )
                    }
                    viewModel.clearNavEvent()
                }
                SettingsNavEvent.SignedOut -> {
                    AuthSession.openSignedOut(requireContext())
                    viewModel.clearNavEvent()
                }
                null -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
