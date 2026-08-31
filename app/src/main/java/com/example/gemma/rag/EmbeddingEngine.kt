package com.example.gemma.rag

interface EmbeddingEngine {
    suspend fun generateEmbedding(text: String): FloatArray
}
