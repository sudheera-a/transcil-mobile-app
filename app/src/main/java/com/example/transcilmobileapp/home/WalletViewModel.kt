package com.example.transcilmobileapp.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.transcilmobileapp.R

class WalletViewModel : ViewModel() {

    private val _overview = MutableLiveData(WalletRepository.overview())
    val overview: LiveData<WalletOverview> = _overview

    private val _transactions = MutableLiveData(WalletRepository.recentTransactions())
    val transactions: LiveData<List<WalletTransaction>> = _transactions

    private val _toastMessage = MutableLiveData<Int?>()
    val toastMessage: LiveData<Int?> = _toastMessage

    fun onWithdraw() {
        _toastMessage.value = R.string.wallet_withdraw_stub
    }

    fun onViewAll() {
        _toastMessage.value = R.string.wallet_view_all_stub
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
