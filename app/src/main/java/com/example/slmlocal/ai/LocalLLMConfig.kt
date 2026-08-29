package com.example.slmlocal.ai

data class LocalLLMConfig(
    override val modelPath: String,
    override val contextSize: Int = 4096,
    override val temperature: Float = 0.7f
) : LLMConfig
