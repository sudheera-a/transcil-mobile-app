package com.example.transcilmobileapp.kyc

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.transcilmobileapp.R

class AadhaarOtpViewModel(application: Application) : AndroidViewModel(application) {

    private val _resendSeconds = MutableLiveData(60)
    val resendSeconds: LiveData<Int> = _resendSeconds

    private val _canResend = MutableLiveData(false)
    val canResend: LiveData<Boolean> = _canResend

    private val _navigateNext = MutableLiveData<Boolean>()
    val navigateNext: LiveData<Boolean> = _navigateNext

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

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
            override fun onTick(millisUntilFinished: Long) {
                _resendSeconds.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                _resendSeconds.value = 0
                _canResend.value = true
            }
        }.start()
    }

    override fun onCleared() {
        timer?.cancel()
        super.onCleared()
    }
}
