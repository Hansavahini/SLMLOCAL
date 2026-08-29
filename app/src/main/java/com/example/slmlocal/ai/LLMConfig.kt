package com.example.slmlocal.ai

interface LLMConfig {
    val modelPath: String
    val contextSize: Int
    val temperature: Float
}
