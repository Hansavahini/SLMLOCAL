package com.example.slmlocal.rag

interface Retriever {
    suspend fun retrieveContext(query: String, maxChunks: Int = 3): List<DocumentChunk>
}
