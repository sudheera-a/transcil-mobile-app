/**
 * WebView screen for API-fetched legal/help HTML (terms, privacy, help center).
 * [ARG_PAGE] nav argument selects which [ContentPage] to load.
 */
package com.transcil.rider.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.transcil.rider.databinding.FragmentApiContentBinding

class ApiContentFragment : Fragment() {

    private var _binding: FragmentApiContentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ApiContentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApiContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val page = arguments?.getString(ARG_PAGE)
            ?.let { runCatching { ContentPage.valueOf(it) }.getOrNull() }
            ?: ContentPage.HELP

        binding.tvTitle.setText(page.titleRes())
        binding.webContent.webViewClient = WebViewClient()

        val goBack = { findNavController().navigateUp() }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goBack()
                }
            }
        )
        binding.btnBack.setOnClickListener { goBack() }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progress.isVisible = loading == true
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.html.observe(viewLifecycleOwner) { html ->
            if (html != null) {
                binding.webContent.loadDataWithBaseURL(
                    null,
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }

        viewModel.load(page)
    }

    override fun onDestroyView() {
        binding.webContent.stopLoading()
        _binding = null
        super.onDestroyView()
    }

    // Navigation-safe argument key shared with nav graph and callers.
    companion object {
        const val ARG_PAGE = "page"
    }
}
