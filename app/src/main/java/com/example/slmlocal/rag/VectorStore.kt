package com.example.slmlocal.rag

interface VectorStore {
    suspend fun addChunk(chunk: DocumentChunk, embedding: FloatArray)
    suspend fun search(queryEmbedding: FloatArray, topK: Int = 3): List<DocumentChunk>
}
