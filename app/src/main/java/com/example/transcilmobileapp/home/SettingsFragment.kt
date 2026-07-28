package com.example.transcilmobileapp.home

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
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.auth.AuthSession
import com.example.transcilmobileapp.databinding.FragmentSettingsBinding

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
