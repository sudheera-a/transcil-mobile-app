/**
 * PAN verification screen for the 3PL journey — optional step between Other Docs and Bank.
 * Verifies PAN against rider name via API; skip sends rider home with KYC still pending on server.
 */
package com.transcil.rider.kyc

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.KycNavigator
import com.transcil.rider.core.KycStatus
import com.transcil.rider.core.UiFormHelpers
import com.transcil.rider.databinding.ActivityPanVerificationBinding

class PanVerificationActivity :
    BaseActivity<ActivityPanVerificationBinding>(ActivityPanVerificationBinding::inflate) {

    private val viewModel: PanVerificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UiFormHelpers.bindStepProgress(binding.stepProgress, activeStep = 4)
        UiFormHelpers.bindFocusHighlight(binding.etPan)

        binding.ivBack.setOnClickListener { finish() }
        binding.tvSkip.setOnClickListener { viewModel.onSkipClicked() }
        binding.tvSkipForNow.setOnClickListener { viewModel.onSkipClicked() }
        binding.btnVerify.setOnClickListener {
            viewModel.onVerifyClicked(binding.etPan.text.toString())
        }

        viewModel.navigateNext.observe(this) { go ->
            if (go == true) {
                KycFlowNavigator.openProgress(this)
            }
        }
        viewModel.skipToHome.observe(this) { skip ->
            if (skip == true) {
                KycNavigator.openHomeDashboard(this, KycStatus.PENDING)
                finish()
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.warningMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                viewModel.clearWarning()
            }
        }
    }
}
