package com.example.gemma.ai

data class LocalLLMConfig(
    override val modelPath: String,
    override val contextSize: Int = 4096,
    override val threads: Int = 4,
    override val temperature: Float = 0.7f
) : LLMConfig
