package com.example.transcilmobileapp.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.transcilmobileapp.databinding.ActivityMainBinding

import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.onboarding.Onboarding1Activity

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, Onboarding1Activity::class.java))
            finish()
        }, 2000)
    }
}