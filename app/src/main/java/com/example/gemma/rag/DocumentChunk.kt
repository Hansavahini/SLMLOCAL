package com.example.gemma.rag

data class DocumentChunk(
    val chunkId: String,
    val documentId: String,
    val text: String,
    val pageNumber: Int? = null,
    val chapter: String? = null,
    val section: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
