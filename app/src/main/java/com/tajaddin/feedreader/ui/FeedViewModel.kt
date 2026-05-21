package com.tajaddin.feedreader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tajaddin.feedreader.data.Article
import com.tajaddin.feedreader.data.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(private val repo: ArticleRepository) : ViewModel() {

    val articles: StateFlow<List<Article>> =
        repo.observeArticles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            _lastError.value = null
            try {
                repo.refresh()
            } catch (e: Exception) {
                // Offline-first: a failed refresh leaves the cached feed intact.
                _lastError.value = e.message ?: "refresh failed"
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun toggleRead(article: Article) {
        viewModelScope.launch { repo.setRead(article.id, !article.isRead) }
    }

    fun toggleBookmark(article: Article) {
        viewModelScope.launch { repo.setBookmarked(article.id, !article.isBookmarked) }
    }
}
