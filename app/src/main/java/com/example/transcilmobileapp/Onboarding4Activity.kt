package com.example.transcilmobileapp

import android.os.Bundle
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityOnboarding4Binding
import android.content.Intent

class Onboarding4Activity : BaseActivity<ActivityOnboarding4Binding>(ActivityOnboarding4Binding::inflate) {

    private val viewModel: Onboarding4ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnGetStarted.setOnClickListener {
            viewModel.onGetStartedClicked()
        }

        binding.tvSkip.setOnClickListener {
            viewModel.onSkipClicked()
        }

        viewModel.navigateToWelcome.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
        }
    }
}


