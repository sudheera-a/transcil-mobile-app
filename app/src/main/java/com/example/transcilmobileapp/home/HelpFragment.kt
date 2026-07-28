package com.example.transcilmobileapp.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.transcilmobileapp.core.FeedbackUi
import com.example.transcilmobileapp.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {
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
