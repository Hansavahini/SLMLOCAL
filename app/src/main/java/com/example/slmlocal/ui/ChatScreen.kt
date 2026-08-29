package com.example.slmlocal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.example.slmlocal.ai.EngineState
import com.example.slmlocal.ui.components.MessageBubble

import com.example.slmlocal.rag.ingestion.IngestionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, ingestionViewModel: IngestionViewModel) {
    var inputText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val engineState by viewModel.engineState.collectAsState()
    val ingestionState by ingestionViewModel.ingestionState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                ingestionViewModel.startIngestion(uri)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SLMLOCAL", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (engineState is EngineState.Ready) Color.Green else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val statusText = when (engineState) {
                            is EngineState.Ready -> "Offline"
                            is EngineState.Loading -> "Loading..."
                            is EngineState.Error -> "Error"
                            else -> "Initializing"
                        }
                        Text(
                            text = statusText, 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { launcher.launch(arrayOf("application/pdf")) }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Import PDF")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            
            if (ingestionState !is IngestionState.Idle) {
                AlertDialog(
                    onDismissRequest = { 
                        if (ingestionState is IngestionState.Completed || ingestionState is IngestionState.Error) {
                            ingestionViewModel.resetState()
                        }
                    },
                    title = { Text("Knowledge Base Import") },
                    text = {
                        when (val state = ingestionState) {
                            is IngestionState.Parsing -> {
                                Column {
                                    LinearProgressIndicator(progress = state.progress)
                                    Spacer(Modifier.height(8.dp))
                                    Text(state.currentActivity)
                                }
                            }
                            is IngestionState.Completed -> {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    Text("Source Pages: ${state.pageCount}")
                                    Text("Final Chunks: ${state.chunkCount}")
                                    Text("Total Chars: ${state.totalCharacters}")
                                    Text("Avg Chunk: ${state.averageChunkSize} chars")
                                    Text("Min Chunk: ${state.minChunkSize} chars")
                                    Text("Max Chunk: ${state.maxChunkSize} chars")
                                    Text("Avg Overlap: ~${state.averageOverlap} chars")
                                    Text("Chapters: ${state.detectedChapters}")
                                    Spacer(Modifier.height(8.dp))
                                    Text("Chapter Names:\n${state.chapterNames.joinToString()}")
                                }
                            }
                            is IngestionState.Error -> {
                                Text("Error: ${state.message}")
                            }
                            else -> {}
                        }
                    },
                    confirmButton = {
                        if (ingestionState is IngestionState.Completed || ingestionState is IngestionState.Error) {
                            TextButton(onClick = { ingestionViewModel.resetState() }) {
                                Text("Close")
                            }
                        }
                    }
                )
            }

            if (engineState is EngineState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = (engineState as EngineState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Send a message to start.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(messages) { message ->
                    MessageBubble(message = message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message SLMLOCAL...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
