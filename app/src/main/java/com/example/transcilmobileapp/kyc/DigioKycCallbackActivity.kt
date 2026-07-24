package com.example.transcilmobileapp.kyc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.transcilmobileapp.repository.DigioKycRepository
import kotlinx.coroutines.launch

class DigioKycCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            DigioKycRepository().syncStatus()
                .onSuccess { data ->
                    if (data.status.equals("approved", ignoreCase = true)) {
                        KycProgressRepository.markCompleted(KycStep.AADHAAR)
                        KycProgressRepository.markCompleted(KycStep.BANK)
                    } else {
                        Toast.makeText(
                            this@DigioKycCallbackActivity,
                            "KYC verification is still pending",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
                .onFailure {
                    Toast.makeText(
                        this@DigioKycCallbackActivity,
                        "Could not sync KYC status",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            openProgressAndFinish()
        }
    }

    private fun openProgressAndFinish() {
        startActivity(
            Intent(this, KycProgressActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
        )
        finish()
    }
}
