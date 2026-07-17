package com.example.transcilmobileapp.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityOnboarding2Binding

import com.example.transcilmobileapp.core.BaseActivity

class Onboarding2Activity : BaseActivity<ActivityOnboarding2Binding>(ActivityOnboarding2Binding::inflate) {

    private val viewModel: Onboarding2ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnNext.setOnClickListener {
            viewModel.onNextClicked()
        }

        binding.tvSkip.setOnClickListener {
            viewModel.onSkipClicked()
        }

        viewModel.navigateToOnboarding3.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                startActivity(Intent(this, Onboarding3Activity::class.java))
            }
        }

        viewModel.skipOnboarding.observe(this) { shouldSkip ->
            if (shouldSkip) {
                // Later: navigate straight to Welcome/Login screen
            }
        }
    }
}