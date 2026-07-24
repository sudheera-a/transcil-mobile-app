package com.example.transcilmobileapp

import android.app.Application
import com.example.transcilmobileapp.data.local.TokenStore

class TranscilApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
    }
}
