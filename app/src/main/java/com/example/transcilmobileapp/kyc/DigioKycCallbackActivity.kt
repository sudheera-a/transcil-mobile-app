package com.example.transcilmobileapp.kyc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.FeedbackUi
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
                        FeedbackUi.toast(
                            this@DigioKycCallbackActivity,
                            getString(R.string.digio_kyc_pending),
                        )
                    }
                }
                .onFailure {
                    FeedbackUi.toast(
                        this@DigioKycCallbackActivity,
                        getString(R.string.digio_kyc_sync_failed),
                    )
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
