package com.example.gemma.rag

class DefaultRetriever(
    private val vectorStore: VectorStore,
    private val embeddingEngine: EmbeddingEngine
) : Retriever {

    override suspend fun retrieveContext(query: String, documentId: String, maxChunks: Int): List<DocumentChunk> {
        val queryEmbedding = embeddingEngine.generateEmbedding(query)
        try {
            android.util.Log.d("RAG_TEST", "query_embedding_dimension=${queryEmbedding.size}")
        } catch (e: Exception) {
            // ignore in tests
        }
        return vectorStore.search(queryEmbedding, documentId, maxChunks)
    }
}
