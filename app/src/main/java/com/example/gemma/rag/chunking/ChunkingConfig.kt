package com.example.gemma.rag.chunking

data class ChunkingConfig(
    val targetChunkSize: Int = 1000,
    val maxChunkSize: Int = 1200,
    val minChunkSize: Int = 50,
    val overlapSize: Int = 200
)
