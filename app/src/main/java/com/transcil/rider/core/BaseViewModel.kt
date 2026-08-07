/**
 * Shared base for screen ViewModels: loading spinner flag + error message for the UI.
 *
 * Architecture notes (MVVM):
 * - Activity/Fragment = UI (views). ViewModel = screen logic + state.
 * - UI observes LiveData; when values change, the UI updates.
 *
 * Kotlin notes:
 * - [LiveData] = lifecycle-aware observable value (UI auto-stops observing when destroyed).
 * - [MutableLiveData] = ViewModel can write; expose read-only [LiveData] so UI cannot write.
 * - Underscore prefix `_isLoading` = private mutable; public `isLoading` = safe read-only view.
 * - `protected fun` = subclasses (WelcomeViewModel, etc.) can call; outsiders cannot.
 */
package com.transcil.rider.core

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    protected fun showLoading() {
        _isLoading.value = true
    }

    protected fun hideLoading() {
        _isLoading.value = false
    }

    protected fun showError(message: String) {
        _errorMessage.value = message
    }
}
