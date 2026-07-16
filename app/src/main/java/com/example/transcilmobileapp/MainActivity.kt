package com.example.transcilmobileapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.transcilmobileapp.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, Onboarding1Activity::class.java))
            finish()
        }, 2000)
    }
}