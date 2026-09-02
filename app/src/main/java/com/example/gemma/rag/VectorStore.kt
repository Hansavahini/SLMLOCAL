package com.example.gemma.rag

interface VectorStore {
    suspend fun addChunk(chunk: DocumentChunk, embedding: FloatArray)
    suspend fun search(queryEmbedding: FloatArray, documentId: String, topK: Int = 3): List<DocumentChunk>
    suspend fun hasDocument(documentId: String): Boolean
    suspend fun removeDocument(documentId: String)
    suspend fun getVectorCount(): Int
}
