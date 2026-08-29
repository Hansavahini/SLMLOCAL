package com.example.slmlocal.ai

data class LocalLLMConfig(
    val modelPath: String,
    val contextSize: Int = 4096,
    val temperature: Float = 0.7f
)
