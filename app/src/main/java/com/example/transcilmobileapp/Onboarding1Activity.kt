package com.example.transcilmobileapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class Onboarding1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding1)

        val btnNext = findViewById<android.widget.Button>(R.id.btnNext)
        btnNext.setOnClickListener {
            btnNext.setOnClickListener {
                startActivity(Intent(this, Onboarding2Activity::class.java))
            }
        }
    }
}