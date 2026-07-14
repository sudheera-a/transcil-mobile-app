package com.example.transcilmobileapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Onboarding4Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding4)

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        val tvSkip = findViewById<TextView>(R.id.tvSkip)

        btnGetStarted.setOnClickListener {
            // Later: navigate to Welcome screen
        }

        tvSkip.setOnClickListener {
            // Later: jump straight to Welcome/Login screen
        }
    }
}