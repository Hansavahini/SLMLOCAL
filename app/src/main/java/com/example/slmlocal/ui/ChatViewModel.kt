package com.example.slmlocal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.slmlocal.ai.EngineState
import com.example.slmlocal.ai.LocalLLMConfig
import com.example.slmlocal.ai.LocalLLMEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import com.example.slmlocal.ai.LlamaJNI

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = LocalLLMEngine()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    init {
        // Force LlamaJNI initialization for Step 3A validation
        val loaded = LlamaJNI.isLoaded

        // Prepare model directory path
        val modelDir = File(application.filesDir, "models")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        val modelPath = File(modelDir, "Phi-3-mini-4k-instruct-q4.gguf").absolutePath

        val config = LocalLLMConfig(
            modelPath = modelPath,
            contextSize = 1024 // Reduced from 4096 to prevent silent OOM kills
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

    private var isGenerating = false

    fun sendMessage(text: String) {
        if (text.isBlank() || isGenerating) return

        isGenerating = true

        // Add user message and a placeholder for the assistant's response
        val userMsg = ChatMessage(text = text, isUser = true)
        val assistantPlaceholder = ChatMessage(text = "Thinking...", isUser = false)
        _messages.value = _messages.value + userMsg + assistantPlaceholder

        // If the user says "Hello" and the state is Error because of missing native lib, 
        // we can still provide the minimal test path as requested by the user, if we want to bypass it.
        // But the user requested: "When the model is available locally...". So we strictly depend on the engine.
        // Since we don't have the model/native library actually compiling yet (it's a stub), we will show the error.
        
        viewModelScope.launch {
            if (engine.isReady()) {
                try {
                    android.util.Log.i("ChatViewModel", "Starting generation for prompt: $text")
                    var firstToken = true
                    engine.generateStream(text).collect { token ->
                        android.util.Log.i("ChatViewModel", "Received token from flow: $token")
                        
                        val currentList = _messages.value.toMutableList()
                        val lastMsg = currentList.last()
                        if (!lastMsg.isUser) {
                            if (firstToken) {
                                // Replace "Thinking..." with the actual first token
                                currentList[currentList.lastIndex] = lastMsg.copy(text = token)
                                firstToken = false
                            } else {
                                currentList[currentList.lastIndex] = lastMsg.copy(text = lastMsg.text + token)
                            }
                            _messages.value = currentList
                        }
                    }
                    android.util.Log.i("ChatViewModel", "Generation flow completed")
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Error during generation", e)
                    _messages.value = _messages.value + ChatMessage(text = "Error: ${e.message}", isUser = false)
                } finally {
                    isGenerating = false
                }
            } else {
                isGenerating = false
                // If not ready, explain why
                val msg = when (val state = engine.state) {
                    is EngineState.Error -> state.message
                    EngineState.Loading -> "Engine is still loading..."
                    else -> "Engine is not ready."
                }
                _messages.value = _messages.value + ChatMessage(text = "System: $msg", isUser = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}
