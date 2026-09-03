package com.example.gemma.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import com.example.gemma.ai.EngineState
import com.example.gemma.ui.components.MessageBubble
import com.example.gemma.rag.ingestion.IngestionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel, 
    ingestionViewModel: IngestionViewModel,
    onNavigateToBooks: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val engineState by viewModel.engineState.collectAsState()
    val ingestionState by ingestionViewModel.ingestionState.collectAsState()
    val isImportingModel by viewModel.isImportingModel.collectAsState()
    val activeDocumentName by viewModel.activeDocumentName.collectAsState()
    
    val conversations by viewModel.conversations.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()

    val modelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.importModel(uri)
            }
        }
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                ingestionViewModel.startIngestion(uri)
            }
        }
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.startNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                    Spacer(Modifier.width(8.dp))
                    Text("New Chat")
                }
                Button(
                    onClick = {
                        onNavigateToBooks()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "My Books")
                    Spacer(Modifier.width(8.dp))
                    Text("My Books")
                }
                Divider()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(conversations) { conv ->
                        val isSelected = conv.id == currentConversationId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.openConversation(conv.id)
                                    scope.launch { drawerState.close() }
                                }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conv.title.ifBlank { "New Chat" },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = dateFormatter.format(Date(conv.updatedAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.deleteConversation(conv.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("gemma", fontWeight = FontWeight.Bold)
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
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        TextButton(onClick = { modelLauncher.launch(arrayOf("*/*")) }) {
                            Text("Import Model")
                        }
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
                if (activeDocumentName != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Active Document: $activeDocumentName",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                if (isImportingModel) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text("Importing Model") },
                        text = {
                            Column {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Copying model file, please wait. This can take a minute...")
                            }
                        },
                        confirmButton = { }
                    )
                }
                
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
                                TextButton(onClick = { 
                                    val state = ingestionState
                                    if (state is IngestionState.Completed) {
                                        viewModel.setActiveDocument(state.documentId, state.documentName)
                                    }
                                    ingestionViewModel.resetState() 
                                }) {
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

                val listState = rememberLazyListState()
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .simpleVerticalScrollbar(listState),
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
                            placeholder = { Text("Message gemma...") },
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
}

@Composable
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp
): Modifier {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration)
    )

    return drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || alpha > 0.0f

        if (needDrawScrollbar && firstVisibleElementIndex != null && state.layoutInfo.totalItemsCount > 0) {
            val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
            val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
            val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight

            drawRect(
                color = Color.Gray,
                topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
                alpha = alpha
            )
        }
    }
}
