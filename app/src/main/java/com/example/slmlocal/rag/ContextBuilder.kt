package com.example.slmlocal.rag

interface ContextBuilder {
    fun buildPrompt(query: String, contextChunks: List<DocumentChunk>): String
}
