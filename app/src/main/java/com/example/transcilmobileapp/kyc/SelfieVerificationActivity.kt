package com.example.transcilmobileapp.kyc

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.KycNavigator
import com.example.transcilmobileapp.databinding.ActivitySelfieVerificationBinding

class SelfieVerificationActivity :
    BaseActivity<ActivitySelfieVerificationBinding>(ActivitySelfieVerificationBinding::inflate) {

    private val viewModel: SelfieVerificationViewModel by viewModels()

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val mime = contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@registerForActivityResult
        viewModel.onCaptured(bytes, mime)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.ivBack.setOnClickListener { finish() }
        binding.btnCapture.setOnClickListener { pickImage.launch("image/*") }
        binding.btnRetake.setOnClickListener { viewModel.onRetake() }
        binding.btnContinue.setOnClickListener { viewModel.onContinue() }

        viewModel.uiState.observe(this) { state ->
            applyUiState(state ?: SelfieUiState.CAPTURE)
        }
        viewModel.openDocumentsStatus.observe(this) { status ->
            if (status != null) {
                KycNavigator.openForStatus(this, status)
                viewModel.clearOpenDocumentsStatus()
                finish()
            }
        }
        viewModel.toastMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearToastMessage()
            }
        }
    }

    private fun applyUiState(state: SelfieUiState) {
        val isCapture = state == SelfieUiState.CAPTURE
        binding.captureHints.visibility = if (isCapture) View.VISIBLE else View.GONE
        binding.btnCapture.visibility = if (isCapture) View.VISIBLE else View.GONE
        binding.reviewStatus.visibility = if (isCapture) View.GONE else View.VISIBLE
        binding.btnContinue.visibility = if (isCapture) View.GONE else View.VISIBLE
        binding.btnRetake.visibility = if (isCapture) View.GONE else View.VISIBLE
    }
}
