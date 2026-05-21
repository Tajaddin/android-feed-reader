package com.tajaddin.feedreader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.tajaddin.feedreader.data.Article

@Composable
fun FeedListScreen(vm: FeedViewModel, onOpen: (String) -> Unit) {
    val articles by vm.articles.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val error by vm.lastError.collectAsState()

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Feed (${articles.size})", style = MaterialTheme.typography.titleLarge)
                Button(onClick = vm::refresh, enabled = !refreshing) {
                    Text(if (refreshing) "Refreshing…" else "Refresh")
                }
            }
            error?.let { Text("Offline: showing cached feed ($it)", color = MaterialTheme.colorScheme.error) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(articles, key = { it.id }) { a ->
                    ArticleRow(a, onOpen = { onOpen(a.id) }, onBookmark = { vm.toggleBookmark(a) })
                }
            }
        }
    }
}

@Composable
private fun ArticleRow(article: Article, onOpen: () -> Unit, onBookmark: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            article.title,
            modifier = Modifier.weight(1f),
            fontWeight = if (article.isRead) FontWeight.Normal else FontWeight.Bold,
        )
        Text(if (article.isBookmarked) "★" else "☆", modifier = Modifier.clickable(onClick = onBookmark))
    }
}

@Composable
fun ArticleDetailScreen(article: Article?, onToggleRead: () -> Unit, onBack: () -> Unit) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (article == null) {
                Text("Article not found")
            } else {
                Text(article.title, style = MaterialTheme.typography.headlineSmall)
                Text(article.summary)
                Text(article.url, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onToggleRead) { Text(if (article.isRead) "Mark unread" else "Mark read") }
                    Button(onClick = onBack) { Text("Back") }
                }
            }
        }
    }
}
