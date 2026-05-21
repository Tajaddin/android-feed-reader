package com.tajaddin.feedreader.ui

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.tajaddin.feedreader.data.ArticleRepository
import com.tajaddin.feedreader.data.local.AppDatabase
import com.tajaddin.feedreader.data.remote.KtorFeedApi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as FeedApp).repository
        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                val vm: FeedViewModel = viewModel(factory = factoryFor(repo))
                NavHost(navController = nav, startDestination = "feed") {
                    composable("feed") {
                        FeedListScreen(vm, onOpen = { id -> nav.navigate("article/$id") })
                    }
                    composable("article/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")
                        val articles by vm.articles.collectAsState()
                        val article = articles.find { it.id == id }
                        ArticleDetailScreen(
                            article = article,
                            onToggleRead = { article?.let(vm::toggleRead) },
                            onBack = { nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

/** Minimal manual DI so the app has no DI framework dependency. */
class FeedApp : Application() {
    val repository: ArticleRepository by lazy {
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "feed.db").build()
        ArticleRepository(db.articleDao(), KtorFeedApi(baseUrl = "https://example.com"))
    }
}

private fun factoryFor(repo: ArticleRepository) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FeedViewModel(repo) as T
}
