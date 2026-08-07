/**
 * Saves login tokens on the device so the user stays signed in across app restarts.
 *
 * Kotlin notes in this file:
 * - `object` = singleton (one shared instance for the whole app).
 * - `private` = only code inside this file/type can use it.
 * - `fun` = function/method.
 * - `@Volatile` / `@Synchronized` = thread-safety helpers for multithreaded reads/writes.
 *
 * Android storage notes:
 * - [SharedPreferences] = simple key→value storage on disk (like a tiny local dictionary).
 * - Plain SharedPreferences stores values readable on a rooted device.
 * - [EncryptedSharedPreferences] = same API, but keys/values are encrypted using a [MasterKey].
 */
package com.transcil.rider.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenStore {
    // `const val` = compile-time constant (string key names for prefs).
    private const val PREFS = "transcil_tokens"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"

    // `var` = mutable; `?` = nullable (can be null before init / when logged out).
    // In-memory copies avoid hitting disk on every API call.
    @Volatile private var prefs: SharedPreferences? = null
    @Volatile private var memAccess: String? = null
    @Volatile private var memRefresh: String? = null

//    /** Call once from [com.transcil.rider.TranscilApp] so prefs exist before any network call. */
    fun init(context: Context) {
        if (prefs != null) return
        // MasterKey: Android Keystore-backed key used to encrypt the preferences file.
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
        // Warm memory cache from encrypted disk.
        memAccess = prefs?.getString(KEY_ACCESS, null)
        memRefresh = prefs?.getString(KEY_REFRESH, null)
    }

    /** Persist access token (and optional refresh token) after login / refresh. */
    @Synchronized
    fun save(accessToken: String, refreshToken: String? = null) {
        memAccess = accessToken
        if (refreshToken != null) memRefresh = refreshToken
        // KTX SharedPreferences.edit { }: lambda editor; commits with apply() by default.
        prefs?.edit {
            putString(KEY_ACCESS, accessToken)
            if (refreshToken != null) putString(KEY_REFRESH, refreshToken)
        }
    }

    fun getAccessToken(): String? = memAccess ?: prefs?.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = memRefresh ?: prefs?.getString(KEY_REFRESH, null)

    /** Wipe tokens on logout or failed refresh. */
    @Synchronized
    fun clear() {
        memAccess = null
        memRefresh = null
        prefs?.edit { clear() }
    }

    fun hasToken(): Boolean = !getAccessToken().isNullOrBlank()
}
