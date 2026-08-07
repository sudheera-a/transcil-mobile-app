/**
 * Application class — created once when the process starts (before any Activity).
 * Initializes encrypted local stores so tokens/KYC drafts are ready app-wide.
 *
 * Wired in AndroidManifest via android:name=".TranscilApp".
 *
 * `override fun onCreate` = Android lifecycle; we must call super.onCreate() first.
 */
package com.transcil.rider

import android.app.Application
import com.transcil.rider.data.local.KycLocalStore
import com.transcil.rider.data.local.TokenStore

class TranscilApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
        KycLocalStore.init(this)
    }
}
