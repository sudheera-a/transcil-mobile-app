package com.example.transcilmobileapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Onboarding3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding3)

        val btnNext = findViewById<Button>(R.id.btnNext)
        val tvSkip = findViewById<TextView>(R.id.tvSkip)

        btnNext.setOnClickListener {
            startActivity(Intent(this, Onboarding4Activity::class.java))
        }

        tvSkip.setOnClickListener {
            // Later: jump straight to Welcome/Login screen
        }
    }
}