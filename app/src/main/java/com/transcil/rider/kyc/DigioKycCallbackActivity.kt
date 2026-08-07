/**
 * Deep-link entry point when Digio redirects back to the app after e-KYC (registered in manifest).
 * Runs [DigioReturnSync], shows optional toast, then opens KycProgressActivity and finishes.
 */
package com.transcil.rider.kyc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.transcil.rider.R
import com.transcil.rider.core.FeedbackUi
import kotlinx.coroutines.launch

class DigioKycCallbackActivity : AppCompatActivity() {

    /** `override fun`: no layout — immediately syncs in a coroutine then navigates away. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // `suspend fun` DigioReturnSync.applyAfterReturn — must run inside coroutine.
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
