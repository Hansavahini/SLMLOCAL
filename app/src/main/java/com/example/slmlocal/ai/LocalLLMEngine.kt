package com.example.slmlocal.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.withContext
import java.io.File

class LocalLLMEngine : LLMEngine {

    private var _state: EngineState = EngineState.Uninitialized
    override val state: EngineState
        get() = _state

    private var contextPtr: Long = 0L

    override suspend fun initialize(config: LLMConfig): EngineState = withContext(Dispatchers.IO) {
        _state = EngineState.Loading

        val modelFile = File(config.modelPath)
        if (!modelFile.exists()) {
            _state = EngineState.Error("Model file missing at: ${config.modelPath}\nPlease place the model file in this directory.")
            return@withContext _state
        }

        if (!LlamaJNI.isLoaded) {
            _state = EngineState.Error("Native library 'llama' is not loaded. C++ build required.")
            return@withContext _state
        }

        try {
            // Attempt to load via JNI
            contextPtr = LlamaJNI.loadModel(config.modelPath, config.contextSize)
            if (contextPtr == 0L) {
                _state = EngineState.Error("Failed to initialize model context natively.")
            } else {
                _state = EngineState.Ready
            }
        } catch (e: Exception) {
            _state = EngineState.Error("Error during initialization: ${e.message}")
        }

        return@withContext _state
    }

    override fun isReady(): Boolean = _state is EngineState.Ready

    override fun generateStream(prompt: String): Flow<String> = kotlinx.coroutines.flow.callbackFlow {
        if (_state !is EngineState.Ready) {
            close(IllegalStateException("Engine is not ready"))
            return@callbackFlow
        }
        android.util.Log.i("LocalLLMEngine", "generateStream started on thread: ${Thread.currentThread().name}")

        if (contextPtr != 0L && LlamaJNI.isLoaded) {
            val callback = object : LlamaJNI.TokenCallback {
                override fun onToken(token: String) {
                    trySend(token)
                }
            }
            
            // Run the blocking JNI generation on a dedicated background thread
            // to completely avoid starving the coroutine dispatcher and triggering ANRs.
            Thread {
                try {
                    android.util.Log.i("LocalLLMEngine", "Calling native generateTokens...")
                    LlamaJNI.generateTokens(contextPtr, prompt, callback)
                    android.util.Log.i("LocalLLMEngine", "Native generateTokens returned.")
                    close()
                } catch (e: Exception) {
                    android.util.Log.e("LocalLLMEngine", "Exception in generateTokens", e)
                    close(e)
                }
            }.start()
            
            // Await close is MANDATORY for callbackFlow to keep the flow open while the thread runs
            awaitClose {
                android.util.Log.i("LocalLLMEngine", "generateStream flow is being closed, cancelling native generation")
                LlamaJNI.cancelGeneration()
            }
        } else {
            close(IllegalStateException("Model pointer invalid or library not loaded."))
        }
    }

    override fun cancel() {
        if (LlamaJNI.isLoaded) {
            LlamaJNI.cancelGeneration()
        }
    }

    override fun release() {
        cancel()
        if (contextPtr != 0L && LlamaJNI.isLoaded) {
            try {
                LlamaJNI.freeModel(contextPtr)
                contextPtr = 0L
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        _state = EngineState.Uninitialized
    }
}
