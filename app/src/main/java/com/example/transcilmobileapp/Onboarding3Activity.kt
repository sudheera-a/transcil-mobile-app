package com.example.transcilmobileapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityOnboarding3Binding

class Onboarding3Activity : BaseActivity<ActivityOnboarding3Binding>(ActivityOnboarding3Binding::inflate) {

    private val viewModel: Onboarding3ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnNext.setOnClickListener {
            viewModel.onNextClicked()
        }

        binding.tvSkip.setOnClickListener {
            viewModel.onSkipClicked()
        }

        viewModel.navigateToOnboarding4.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                startActivity(Intent(this, Onboarding4Activity::class.java))
            }
        }

        viewModel.skipOnboarding.observe(this) { shouldSkip ->
            if (shouldSkip) {
                // Later: navigate straight to Welcome/Login screen
            }
        }
    }
}