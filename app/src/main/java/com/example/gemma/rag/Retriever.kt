package com.example.gemma.rag

interface Retriever {
    suspend fun retrieveContext(query: String, documentId: String, maxChunks: Int = 3): List<DocumentChunk>
}
