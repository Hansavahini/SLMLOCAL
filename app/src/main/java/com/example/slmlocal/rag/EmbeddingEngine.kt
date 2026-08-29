package com.example.slmlocal.rag

interface EmbeddingEngine {
    suspend fun generateEmbedding(text: String): FloatArray
}
