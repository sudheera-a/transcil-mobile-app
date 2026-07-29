package com.example.transcilmobileapp.kyc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.FeedbackUi
import kotlinx.coroutines.launch

class DigioKycCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val outcome = DigioReturnSync.applyAfterReturn()
            when (DigioReturnSync.toastFor(outcome)) {
                DigioReturnSync.ToastKind.SYNC_FAILED ->
                    FeedbackUi.toast(this@DigioKycCallbackActivity, getString(R.string.digio_kyc_sync_failed))
                DigioReturnSync.ToastKind.PENDING ->
                    FeedbackUi.toast(this@DigioKycCallbackActivity, getString(R.string.digio_kyc_pending))
                DigioReturnSync.ToastKind.NONE -> Unit
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
