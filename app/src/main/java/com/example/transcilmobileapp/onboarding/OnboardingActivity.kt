package com.example.transcilmobileapp.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.auth.WelcomeActivity
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.SegmentedStepper
import com.example.transcilmobileapp.databinding.ActivityOnboardingBinding

class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>(ActivityOnboardingBinding::inflate) {

    private val viewModel: OnboardingViewModel by viewModels()

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
