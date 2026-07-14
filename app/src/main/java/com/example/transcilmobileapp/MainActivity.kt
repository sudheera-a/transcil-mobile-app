package com.example.transcilmobileapp// keep whatever your actual package name is

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.example.transcilmobileapp.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Wait 2 seconds, then move to the next screen
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, Onboarding1Activity::class.java))
            finish()
        }, 2000)
    }
}