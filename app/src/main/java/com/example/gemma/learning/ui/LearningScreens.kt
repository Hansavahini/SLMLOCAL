package com.example.gemma.learning.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gemma.ai.LLMEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    viewModel: LearningViewModel,
    onBookClick: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val documents by viewModel.documents.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Books") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            if (documents.isEmpty()) {
                item {
                    Text("No books uploaded yet.", modifier = Modifier.padding(16.dp))
                }
            }
            items(documents) { doc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onBookClick(doc.id, doc.title) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(doc.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Pages: ${doc.totalPages}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersScreen(
    viewModel: LearningViewModel,
    documentId: String,
    documentTitle: String,
    onChapterClick: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val chapters by viewModel.chapters.collectAsState()

    LaunchedEffect(documentId) {
        viewModel.loadChapters(documentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(documentTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(chapters) { chapter ->
                ListItem(
                    headlineContent = { Text(chapter.title, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Pages ${chapter.startPage} - ${chapter.endPage}") },
                    modifier = Modifier.clickable { onChapterClick(chapter.id, chapter.title) }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesScreen(
    viewModel: LearningViewModel,
    chapterId: String,
    chapterTitle: String,
    onModuleClick: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val modules by viewModel.modules.collectAsState()

    LaunchedEffect(chapterId) {
        viewModel.loadModules(chapterId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chapterTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(modules) { module ->
                ListItem(
                    headlineContent = { Text(module.title) },
                    modifier = Modifier.clickable { onModuleClick(module.id, module.title) }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationScreen(
    viewModel: LearningViewModel,
    moduleId: String,
    moduleTitle: String,
    llmEngine: LLMEngine,
    onBack: () -> Unit
) {
    val explanationStream by viewModel.explanationStream.collectAsState()

    LaunchedEffect(moduleId) {
        viewModel.generateExplanation(moduleId, llmEngine)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(moduleTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (explanationStream.isEmpty()) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Generating explanation offline...")
            } else {
                Text(explanationStream)
            }
        }
    }
}
