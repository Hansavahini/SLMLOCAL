package com.example.gemma.ai

import kotlinx.coroutines.flow.Flow

interface LLMEngine {
    val state: EngineState
    suspend fun initialize(config: LLMConfig): EngineState
    fun isReady(): Boolean
    fun generateStream(prompt: String): Flow<String>
    fun cancel()
    fun release()
}
