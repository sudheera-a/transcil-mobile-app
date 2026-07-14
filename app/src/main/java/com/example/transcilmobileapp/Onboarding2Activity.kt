package com.example.transcilmobileapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Onboarding2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding2)

        val btnNext = findViewById<Button>(R.id.btnNext)
        val tvSkip = findViewById<TextView>(R.id.tvSkip)

        btnNext.setOnClickListener {
            startActivity(Intent(this, Onboarding3Activity::class.java))
        }
        tvSkip.setOnClickListener {
            // Later: jump straight to the Welcome/Login screen
        }
    }
}