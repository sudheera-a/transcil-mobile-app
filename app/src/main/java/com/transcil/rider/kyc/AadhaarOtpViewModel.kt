/**
 * ViewModel for the Aadhaar OTP entry screen (legacy/alternate path; Digio is the primary flow).
 * Manages resend countdown via CountDownTimer and exposes one-shot navigation events to the Activity.
 */
package com.transcil.rider.kyc

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.transcil.rider.R

class AadhaarOtpViewModel(application: Application) : AndroidViewModel(application) {

    // ViewModel + LiveData: mutate `_resendSeconds` internally; Activity observes the read-only `resendSeconds`.
    private val _resendSeconds = MutableLiveData(60)
    val resendSeconds: LiveData<Int> = _resendSeconds

    private val _canResend = MutableLiveData(false)
    val canResend: LiveData<Boolean> = _canResend

    private val _navigateNext = MutableLiveData<Boolean>()
    val navigateNext: LiveData<Boolean> = _navigateNext

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // `private var`: mutable reference — reassigned when the resend timer is restarted or cancelled.
    private var timer: CountDownTimer? = null

    init {
        startResendTimer()
    }

    fun onVerifyClicked(otp: String) {
        if (otp.length != 6) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_incomplete_otp)
            return
        }
        _navigateNext.value = true
    }

    fun onResendClicked() {
        if (_canResend.value == true) {
            startResendTimer()
        }
    }

    private fun startResendTimer() {
        timer?.cancel()
        _canResend.value = false
        timer = object : CountDownTimer(60_000, 1_000) {
            // `override fun`: required by CountDownTimer interface — called on the main thread each tick.
            override fun onTick(millisUntilFinished: Long) {
                _resendSeconds.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                _resendSeconds.value = 0
                _canResend.value = true
            }
        }.start()
    }

    /** `override fun`: ViewModel lifecycle hook — cancel timer to avoid leaks after Activity is destroyed. */
    override fun onCleared() {
        timer?.cancel()
        super.onCleared()
    }
}
