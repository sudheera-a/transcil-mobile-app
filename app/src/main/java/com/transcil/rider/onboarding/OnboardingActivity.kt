/**
 * First-run intro carousel — three slides explaining the app before phone login.
 * Shown on cold start for new installs; skip or finish navigates to WelcomeActivity (auth).
 */
package com.transcil.rider.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.transcil.rider.R
import com.transcil.rider.auth.WelcomeActivity
import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.SegmentedStepper
import com.transcil.rider.databinding.ActivityOnboardingBinding

class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>(ActivityOnboardingBinding::inflate) {

    private val viewModel: OnboardingViewModel by viewModels()

    /** `override fun`: carousel page binding — updates hero, title, stepper on each slide change. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnNext.setOnClickListener { viewModel.onNextClicked() }
        binding.btnSkip.setOnClickListener { viewModel.onSkipClicked() }

        viewModel.pageIndex.observe(this) { index ->
            val page = viewModel.pages[index]
            binding.ivHero.setImageResource(page.imageRes)
            binding.tvTitle.setText(page.titleRes)
            binding.tvDesc.setText(page.descRes)
            binding.btnNext.setText(
                if (index == viewModel.pages.lastIndex) R.string.get_started else R.string.next,
            )
            // 3 slides map onto first 3 of 4 segments; last fills on get-started screen intent
            SegmentedStepper.apply(binding.stepperInclude, filledCount = index + 1, navyInactive = true)
        }

        viewModel.navigateToWelcome.observe(this) { go ->
            if (go) {
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            }
        }
    }
}
