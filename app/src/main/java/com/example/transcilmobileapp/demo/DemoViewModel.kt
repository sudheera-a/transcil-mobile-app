package com.example.transcilmobileapp.data.demo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.transcilmobileapp.core.BaseViewModel
import com.example.transcilmobileapp.data.repository.DemoRepository
import kotlinx.coroutines.launch

class DemoViewModel : BaseViewModel() {

    private val repository = DemoRepository()

    private val _postTitle = MutableLiveData<String>()
    val postTitle: LiveData<String> get() = _postTitle

    fun loadPracticePost() {
        viewModelScope.launch {
            showLoading()
            try {
                val post = repository.loadPost(1)
                _postTitle.value = post.title
            } catch (e: Exception) {
                showError(e.message ?: "Network error")
            } finally {
                hideLoading()
            }
        }
    }
}