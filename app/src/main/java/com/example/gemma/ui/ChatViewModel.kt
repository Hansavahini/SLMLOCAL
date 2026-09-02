package com.example.gemma.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemma.ai.EngineState
import com.example.gemma.ai.LocalLLMConfig
import com.example.gemma.ai.LLMEngine
import com.example.gemma.ai.LocalLLMEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import android.content.Context
import com.example.gemma.ai.LlamaJNI
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val engine: LLMEngine = LocalLLMEngine()

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

    init {
        // Initialize RAG dependencies
        com.example.gemma.rag.RagDependencies.init(application)
        
        // Restore active document
        val prefs = application.getSharedPreferences("rag_prefs", Context.MODE_PRIVATE)
        val savedDocId = prefs.getString("active_document_id", null)
        val savedDocName = prefs.getString("active_document_name", null)
        if (savedDocId != null) {
            viewModelScope.launch {
                if (com.example.gemma.rag.RagDependencies.vectorStore.hasDocument(savedDocId)) {
                    _activeDocumentId.value = savedDocId
                    _activeDocumentName.value = savedDocName
                } else {
                    prefs.edit().remove("active_document_id").remove("active_document_name").apply()
                }
            }
        }

        // Force LlamaJNI initialization for Step 3A validation
        val loaded = LlamaJNI.isLoaded

        // Prepare model directory path
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
            threads = 4 // HARDCODED OPTIMIZATION: Using all 8 cores on a mobile CPU actually slows it down because the 4 "efficiency cores" drag down the 4 "performance cores" during synchronization.
        )

        viewModelScope.launch {
            _engineState.value = EngineState.Loading
            
            // Load Knowledge Base (Disconnected RAG)
            // loadKnowledgeBase(application)
            
            val state = engine.initialize(config)
            _engineState.value = state
            if (state is EngineState.Ready) {
                android.util.Log.i("ChatViewModel", "Engine is ready! Waiting for user input.")
            }
        }
    }

    private suspend fun loadKnowledgeBase(application: Application) {
        // RAG functionality disconnected. 
        // Variables like embeddingEngine and vectorStore are commented out.
    }

    private var isGenerating = false

    fun sendMessage(text: String) {
        if (text.isBlank() || isGenerating) return
        
        android.util.Log.d("RAG_TEST", "question_received")

        isGenerating = true

        val userMsg = ChatMessage(text = text, isUser = true)
        val assistantPlaceholder = ChatMessage(text = "Searching and Thinking...", isUser = false)
        _messages.value = _messages.value + userMsg + assistantPlaceholder
        
        viewModelScope.launch {
            if (engine.isReady()) {
                try {
                    android.util.Log.i("ChatViewModel", "Starting generation for prompt: $text")
                    var firstToken = true
                    
                    val docId = _activeDocumentId.value
                    val flow = if (docId != null) {
                        // Use RAG flow
                        val ragEngine = com.example.gemma.rag.DefaultRAGEngine(
                            engine, 
                            com.example.gemma.rag.RagDependencies.retriever, 
                            com.example.gemma.rag.RagDependencies.contextBuilder
                        )
                        com.example.gemma.rag.RagDependencies.ragEngine = ragEngine
                        ragEngine.answerQuestionStream(text, docId)
                    } else {
                        // Use direct local LLM flow
                        engine.generateStream(text)
                    }

                    flow.collect { token ->
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
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Error during generation", e)
                    _messages.value = _messages.value + ChatMessage(text = "Error: ${e.message}", isUser = false)
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
                _messages.value = _messages.value + ChatMessage(text = "System: $msg", isUser = false)
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
                
                // Show success state
                _engineState.value = EngineState.Error("Model imported successfully! Please completely close and restart the app to load it.")
            } catch (e: Exception) {
                _engineState.value = EngineState.Error("Failed to import model: ${e.message}")
            } finally {
                _isImportingModel.value = false
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
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}
