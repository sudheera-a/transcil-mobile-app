package com.example.transcilmobileapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenStore {
    private const val PREFS = "transcil_tokens"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"

    @Volatile private var prefs: SharedPreferences? = null
    @Volatile private var memAccess: String? = null
    @Volatile private var memRefresh: String? = null

    fun init(context: Context) {
        if (prefs != null) return
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        memAccess = prefs?.getString(KEY_ACCESS, null)
        memRefresh = prefs?.getString(KEY_REFRESH, null)
    }

    @Synchronized
    fun save(accessToken: String, refreshToken: String? = null) {
        memAccess = accessToken
        if (refreshToken != null) memRefresh = refreshToken
        prefs?.edit()
            ?.putString(KEY_ACCESS, accessToken)
            ?.apply {
                if (refreshToken != null) putString(KEY_REFRESH, refreshToken)
            }
            ?.apply()
    }

    fun getAccessToken(): String? = memAccess ?: prefs?.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = memRefresh ?: prefs?.getString(KEY_REFRESH, null)

    @Synchronized
    fun clear() {
        memAccess = null
        memRefresh = null
        prefs?.edit()?.clear()?.apply()
    }

    fun hasToken(): Boolean = !getAccessToken().isNullOrBlank()
}
