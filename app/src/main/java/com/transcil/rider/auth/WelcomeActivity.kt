/**
 * First auth screen: user enters a 10-digit mobile number and requests an OTP.
 * Part of the login flow after splash; on success navigates to [VerifyOtpActivity].
 *
 * Kotlin / Android notes:
 * - Extends [BaseActivity] with [ActivityWelcomeBinding] (ViewBinding — type-safe access to XML views).
 * - `by viewModels()` = lazy ViewModel scoped to this Activity (survives config changes).
 * - `observe` = subscribe to LiveData; lambda runs when the ViewModel publishes a new value.
 */
package com.transcil.rider.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import com.transcil.rider.R
import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.FeedbackUi
import com.transcil.rider.core.NavExtras
import com.transcil.rider.core.SegmentedStepper
import com.transcil.rider.databinding.ActivityWelcomeBinding

class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>(ActivityWelcomeBinding::inflate) {

    // ViewModel holds send-OTP logic; Activity only wires UI events and observes results.
    private val viewModel: WelcomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.navyHeaderInclude.findViewById<TextView>(R.id.headerTitle).setText(R.string.welcome_title)
        binding.navyHeaderInclude.findViewById<TextView>(R.id.headerSubtitle).setText(R.string.welcome_subtitle)
        // Step 1 of 4 in the auth/onboarding progress bar.
        SegmentedStepper.apply(binding.navyHeaderInclude, filledCount = 1, navyInactive = true)

        binding.btnSendOtp.setOnClickListener {
            viewModel.onSendOtpClicked(binding.etMobileNumber.text.toString())
        }
        binding.ivBack.setOnClickListener { finish() }

        // One-shot navigation event: mobile + server session id for Verify OTP screen.
        viewModel.navigateToVerifyOtp.observe(this) { event ->
            startActivity(
                Intent(this, VerifyOtpActivity::class.java)
                    .putExtra(NavExtras.MOBILE_NUMBER, event.mobile)
                    .putExtra(NavExtras.OTP_SESSION, event.session),
            )
        }
        viewModel.isLoading.observe(this) { loading ->
            binding.btnSendOtp.isEnabled = loading != true
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) FeedbackUi.toast(this, message)
        }
    }
}
