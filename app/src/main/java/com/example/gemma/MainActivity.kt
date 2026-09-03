package com.example.gemma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.example.gemma.ui.ChatScreen
import com.example.gemma.ui.ChatViewModel
import com.example.gemma.ui.IngestionViewModel
import com.example.gemma.ui.theme.gemmaTheme
import com.example.gemma.learning.ui.*

sealed class AppScreen {
    object Chat : AppScreen()
    object Books : AppScreen()
    data class Chapters(val docId: String, val docTitle: String) : AppScreen()
    data class Modules(val chapterId: String, val chapterTitle: String) : AppScreen()
    data class Explanation(val moduleId: String, val moduleTitle: String) : AppScreen()
}

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val ingestionViewModel: IngestionViewModel by viewModels()
    private val learningViewModel: LearningViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            gemmaTheme {
                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Chat) }

                when (val screen = currentScreen) {
                    is AppScreen.Chat -> {
                        ChatScreen(
                            viewModel = chatViewModel,
                            ingestionViewModel = ingestionViewModel,
                            onNavigateToBooks = { currentScreen = AppScreen.Books }
                        )
                    }
                    is AppScreen.Books -> {
                        BooksScreen(
                            viewModel = learningViewModel,
                            onBookClick = { docId, docTitle -> 
                                currentScreen = AppScreen.Chapters(docId, docTitle) 
                            },
                            onBack = { currentScreen = AppScreen.Chat }
                        )
                    }
                    is AppScreen.Chapters -> {
                        ChaptersScreen(
                            viewModel = learningViewModel,
                            documentId = screen.docId,
                            documentTitle = screen.docTitle,
                            onChapterClick = { chapId, chapTitle ->
                                currentScreen = AppScreen.Modules(chapId, chapTitle)
                            },
                            onBack = { currentScreen = AppScreen.Books }
                        )
                    }
                    is AppScreen.Modules -> {
                        ModulesScreen(
                            viewModel = learningViewModel,
                            chapterId = screen.chapterId,
                            chapterTitle = screen.chapterTitle,
                            onModuleClick = { modId, modTitle ->
                                currentScreen = AppScreen.Explanation(modId, modTitle)
                            },
                            onBack = { currentScreen = AppScreen.Books } // simplify back stack
                        )
                    }
                    is AppScreen.Explanation -> {
                        ExplanationScreen(
                            viewModel = learningViewModel,
                            moduleId = screen.moduleId,
                            moduleTitle = screen.moduleTitle,
                            llmEngine = chatViewModel.engine,
                            onBack = { currentScreen = AppScreen.Books } // simplify back stack
                        )
                    }
                }
            }
        }
    }
}