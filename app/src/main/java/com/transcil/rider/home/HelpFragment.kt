/**
 * Static help/support screen with call, chat, and email stub actions.
 * Distinct from [ApiContentFragment] which loads server HTML in a WebView.
 */
package com.transcil.rider.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.transcil.rider.core.FeedbackUi
import com.transcil.rider.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {
    // ViewBinding pattern: _binding nulled in onDestroyView to avoid retaining the view.
    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnCall.setOnClickListener { FeedbackUi.toast(requireContext(), "Calling roadside…") }
        binding.btnChat.setOnClickListener { FeedbackUi.toast(requireContext(), "Chat online") }
        binding.btnEmail.setOnClickListener { FeedbackUi.toast(requireContext(), "support@transcil.com") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
