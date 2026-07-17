package com.example.transcilmobileapp.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityOnboarding1Binding

import com.example.transcilmobileapp.core.BaseActivity

class Onboarding1Activity : BaseActivity<ActivityOnboarding1Binding>(ActivityOnboarding1Binding::inflate) {

    private val viewModel: Onboarding1ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnNext.setOnClickListener {
            viewModel.onNextClicked()
        }

        viewModel.navigateToOnboarding2.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                startActivity(Intent(this, Onboarding2Activity::class.java))
            }
        }
    }
}