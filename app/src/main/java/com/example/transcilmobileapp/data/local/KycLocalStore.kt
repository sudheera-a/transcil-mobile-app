package com.example.transcilmobileapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.transcilmobileapp.kyc.BankDraft

/**
 * Survives process death for temporary local-only KYC (bank IFSC path).
 * Cleared on logout via [clear].
 */
object KycLocalStore {
    private const val PREFS = "transcil_kyc_local"
    private const val KEY_HOLDER = "bank_holder"
    private const val KEY_ACCOUNT = "bank_account"
    private const val KEY_CONFIRM = "bank_confirm"
    private const val KEY_IFSC = "bank_ifsc"
    private const val KEY_CONSENT = "bank_consent"
    private const val KEY_BANK_SUBTITLE = "bank_completed_subtitle"

    @Volatile private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun saveBank(draft: BankDraft, completedSubtitle: String?) {
        prefs?.edit()
            ?.putString(KEY_HOLDER, draft.holderName)
            ?.putString(KEY_ACCOUNT, draft.accountNumber)
            ?.putString(KEY_CONFIRM, draft.confirmAccountNumber)
            ?.putString(KEY_IFSC, draft.ifsc)
            ?.putBoolean(KEY_CONSENT, draft.consent)
            ?.apply {
                if (completedSubtitle.isNullOrBlank()) {
                    remove(KEY_BANK_SUBTITLE)
                } else {
                    putString(KEY_BANK_SUBTITLE, completedSubtitle)
                }
            }
            ?.apply()
    }

    fun loadBank(): Pair<BankDraft, String?>? {
        val p = prefs ?: return null
        if (!p.contains(KEY_IFSC) && !p.contains(KEY_HOLDER) && !p.contains(KEY_BANK_SUBTITLE)) {
            return null
        }
        val draft = BankDraft(
            holderName = p.getString(KEY_HOLDER, "").orEmpty(),
            accountNumber = p.getString(KEY_ACCOUNT, "").orEmpty(),
            confirmAccountNumber = p.getString(KEY_CONFIRM, "").orEmpty(),
            ifsc = p.getString(KEY_IFSC, "").orEmpty(),
            consent = p.getBoolean(KEY_CONSENT, false),
        )
        return draft to p.getString(KEY_BANK_SUBTITLE, null)
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}
