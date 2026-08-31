package com.example.gemma.rag

interface ContextBuilder {
    fun buildPrompt(query: String, contextChunks: List<DocumentChunk>): String
}
