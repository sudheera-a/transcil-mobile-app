package com.example.transcilmobileapp.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.transcilmobileapp.core.BaseViewModel
import com.example.transcilmobileapp.data.network.ApiClient
import kotlinx.coroutines.launch

class ApiContentViewModel : BaseViewModel() {

    private val _html = MutableLiveData<String>()
    val html: LiveData<String> = _html

    fun load(page: ContentPage) {
        if (_html.value != null) return
        viewModelScope.launch {
            showLoading()
            try {
                _html.value = fetchHtml(page)
            } catch (e: Exception) {
                showError(e.message ?: "Network error")
            } finally {
                hideLoading()
            }
        }
    }

    private suspend fun fetchHtml(page: ContentPage): String {
        val api = ApiClient.transcilApi
        return when (page) {
            ContentPage.HELP -> {
                val data = api.getHelpCenter().data
                    ?: error("Help Center returned empty data")
                HelpCenterHtml.build(data)
            }
            ContentPage.TERMS -> {
                api.getTerms().data?.html?.takeIf { it.isNotBlank() }
                    ?: error("Terms returned empty data")
            }
            ContentPage.PRIVACY -> {
                api.getPrivacy().data?.html?.takeIf { it.isNotBlank() }
                    ?: error("Privacy returned empty data")
            }
            ContentPage.TERMS_PRIVACY -> {
                val terms = api.getTerms().data?.html.orEmpty()
                val privacy = api.getPrivacy().data?.html.orEmpty()
                if (terms.isBlank() && privacy.isBlank()) {
                    error("Terms & Privacy returned empty data")
                }
                buildString {
                    append(terms)
                    if (terms.isNotBlank() && privacy.isNotBlank()) append("<hr/>")
                    append(privacy)
                }
            }
        }
    }
}
