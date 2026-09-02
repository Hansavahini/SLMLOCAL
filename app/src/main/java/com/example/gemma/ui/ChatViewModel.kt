package com.example.gemma.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemma.ai.EngineState
import com.example.gemma.ai.LLMEngine
import com.example.gemma.ai.LlamaJNI
import com.example.gemma.ai.LocalLLMConfig
import com.example.gemma.ai.LocalLLMEngine
import com.example.gemma.db.ChatDatabase
import com.example.gemma.db.ConversationEntity
import com.example.gemma.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val engine: LLMEngine = LocalLLMEngine()
    
    private val database = ChatDatabase.getDatabase(application)
    private val repository = ChatRepository(database.conversationDao(), database.messageDao())

    private val RECENT_MESSAGE_LIMIT = 8

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = _conversations.asStateFlow()

    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId: StateFlow<Long?> = _currentConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _isImportingModel = MutableStateFlow(false)
    val isImportingModel: StateFlow<Boolean> = _isImportingModel.asStateFlow()

    private val _activeDocumentId = MutableStateFlow<String?>(null)
    val activeDocumentId: StateFlow<String?> = _activeDocumentId.asStateFlow()

    private val _activeDocumentName = MutableStateFlow<String?>(null)
    val activeDocumentName: StateFlow<String?> = _activeDocumentName.asStateFlow()
    
    private var isGenerating = false

    init {
        com.example.gemma.rag.RagDependencies.init(application)
        
        viewModelScope.launch {
            loadConversations()
            val allConversations = _conversations.value
            if (allConversations.isNotEmpty()) {
                openConversation(allConversations.first().id)
            } else {
                startNewConversation()
            }
        }

        val loaded = LlamaJNI.isLoaded

        val modelDir = File(application.filesDir, "models")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        val modelPath = File(modelDir, "Llama-3.2-1B-Instruct-Q4_K_M.gguf").absolutePath

        val isEmulator = (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86"))

        val config = LocalLLMConfig(
            modelPath = modelPath,
            contextSize = if (isEmulator) 512 else 2048,
            threads = 4
        )

        viewModelScope.launch {
            _engineState.value = EngineState.Loading
            val state = engine.initialize(config)
            _engineState.value = state
            if (state is EngineState.Ready) {
                android.util.Log.i("ChatViewModel", "Engine is ready! Waiting for user input.")
            }
        }
    }

    private suspend fun loadConversations() {
        _conversations.value = repository.loadConversations()
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("rag_prefs", Context.MODE_PRIVATE)
            val savedDocId = prefs.getString("active_document_id", null)
            val savedDocName = prefs.getString("active_document_name", null)
            
            var docId = savedDocId
            var docName = savedDocName
            if (docId != null && !com.example.gemma.rag.RagDependencies.vectorStore.hasDocument(docId)) {
                docId = null
                docName = null
                prefs.edit().remove("active_document_id").remove("active_document_name").apply()
            }

            val newId = repository.createConversation("New Chat", docId, docName)
            loadConversations()
            openConversation(newId)
        }
    }

    fun openConversation(id: Long) {
        viewModelScope.launch {
            _currentConversationId.value = id
            val conv = repository.getConversation(id)
            if (conv != null) {
                _activeDocumentId.value = conv.activeDocumentId
                _activeDocumentName.value = conv.activeDocumentName
                
                val prefs = getApplication<Application>().getSharedPreferences("rag_prefs", Context.MODE_PRIVATE)
                if (conv.activeDocumentId != null) {
                    if (com.example.gemma.rag.RagDependencies.vectorStore.hasDocument(conv.activeDocumentId)) {
                        prefs.edit().putString("active_document_id", conv.activeDocumentId).putString("active_document_name", conv.activeDocumentName).apply()
                    } else {
                        repository.updateActiveDocument(id, null, null)
                        _activeDocumentId.value = null
                        _activeDocumentName.value = null
                        prefs.edit().remove("active_document_id").remove("active_document_name").apply()
                    }
                } else {
                    prefs.edit().remove("active_document_id").remove("active_document_name").apply()
                }
            }

            val dbMessages = repository.loadFullMessages(id)
            _messages.value = dbMessages.map { 
                ChatMessage(id = it.id.toString(), text = it.content, isUser = it.role == "USER") 
            }
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            val conv = repository.getConversation(id)
            if (conv != null) {
                repository.deleteConversation(conv)
                loadConversations()
                if (_currentConversationId.value == id) {
                    val allConversations = _conversations.value
                    if (allConversations.isNotEmpty()) {
                        openConversation(allConversations.first().id)
                    } else {
                        startNewConversation()
                    }
                }
            }
        }
    }

    fun setActiveDocument(documentId: String?, documentName: String?) {
        _activeDocumentId.value = documentId
        _activeDocumentName.value = documentName
        
        val prefs = getApplication<Application>().getSharedPreferences("rag_prefs", Context.MODE_PRIVATE)
        if (documentId != null) {
            prefs.edit().putString("active_document_id", documentId).putString("active_document_name", documentName).apply()
        } else {
            prefs.edit().remove("active_document_id").remove("active_document_name").apply()
        }

        val currentId = _currentConversationId.value
        if (currentId != null) {
            viewModelScope.launch {
                repository.updateActiveDocument(currentId, documentId, documentName)
                loadConversations()
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isGenerating) return
        val currentId = _currentConversationId.value ?: return

        isGenerating = true

        viewModelScope.launch {
            if (_messages.value.isEmpty()) {
                val newTitle = text.take(30) + if (text.length > 30) "..." else ""
                repository.updateTitle(currentId, newTitle)
                loadConversations()
            }

            repository.saveUserMessage(currentId, text)
            
            val dbMessages = repository.loadFullMessages(currentId)
            _messages.value = dbMessages.map { 
                ChatMessage(id = it.id.toString(), text = it.content, isUser = it.role == "USER") 
            }
            
            _messages.value = _messages.value + ChatMessage(text = "Searching and Thinking...", isUser = false)
            
            val recentEntities = repository.loadRecentMessages(currentId, RECENT_MESSAGE_LIMIT + 1)
            val historyEntities = recentEntities.dropLast(1).takeLast(RECENT_MESSAGE_LIMIT)
            
            val historyString = historyEntities.joinToString("\n") { 
                val roleName = if (it.role == "USER") "User" else "Assistant"
                "$roleName: ${it.content}"
            }

            if (engine.isReady()) {
                try {
                    var firstToken = true
                    val docId = _activeDocumentId.value
                    
                    val flow = if (docId != null) {
                        val ragEngine = com.example.gemma.rag.DefaultRAGEngine(
                            engine, 
                            com.example.gemma.rag.RagDependencies.retriever, 
                            com.example.gemma.rag.RagDependencies.contextBuilder
                        )
                        com.example.gemma.rag.RagDependencies.ragEngine = ragEngine
                        ragEngine.answerQuestionStream(text, docId, historyString)
                    } else {
                        val prompt = if (historyString.isNotBlank()) {
                            "Recent Conversation History:\n$historyString\n\nQuestion: $text\nAnswer:"
                        } else {
                            text
                        }
                        engine.generateStream(prompt)
                    }

                    var fullAssistantResponse = ""
                    flow.collect { token ->
                        fullAssistantResponse += token
                        val currentList = _messages.value.toMutableList()
                        val lastMsg = currentList.last()
                        if (!lastMsg.isUser) {
                            if (firstToken) {
                                currentList[currentList.lastIndex] = lastMsg.copy(text = token)
                                firstToken = false
                            } else {
                                currentList[currentList.lastIndex] = lastMsg.copy(text = lastMsg.text + token)
                            }
                            _messages.value = currentList
                        }
                    }
                    
                    repository.saveAssistantMessage(currentId, fullAssistantResponse)
                    
                    val updatedDbMessages = repository.loadFullMessages(currentId)
                    _messages.value = updatedDbMessages.map { 
                        ChatMessage(id = it.id.toString(), text = it.content, isUser = it.role == "USER") 
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Error during generation", e)
                    _messages.value = _messages.value.dropLast(1) + ChatMessage(text = "Error: ${e.message}", isUser = false)
                } finally {
                    isGenerating = false
                }
            } else {
                isGenerating = false
                val msg = when (val state = engine.state) {
                    is EngineState.Error -> state.message
                    EngineState.Loading -> "Engine is still loading..."
                    else -> "Engine is not ready."
                }
                _messages.value = _messages.value.dropLast(1) + ChatMessage(text = "System: $msg", isUser = false)
            }
        }
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            _isImportingModel.value = true
            try {
                withContext(Dispatchers.IO) {
                    val modelDir = File(getApplication<Application>().filesDir, "models")
                    if (!modelDir.exists()) {
                        modelDir.mkdirs()
                    }
                    val targetFile = File(modelDir, "Llama-3.2-1B-Instruct-Q4_K_M.gguf")
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                _engineState.value = EngineState.Error("Model imported successfully! Please completely close and restart the app to load it.")
            } catch (e: Exception) {
                _engineState.value = EngineState.Error("Failed to import model: ${e.message}")
            } finally {
                _isImportingModel.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}
