package com.example.gemma.ai

interface LLMConfig {
    val modelPath: String
    val contextSize: Int
    val threads: Int
    val temperature: Float
}
