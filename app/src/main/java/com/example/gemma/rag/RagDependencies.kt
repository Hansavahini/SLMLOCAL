package com.example.gemma.rag

import android.content.Context

object RagDependencies {

    lateinit var embeddingEngine: EmbeddingEngine
        private set
        
    lateinit var vectorStore: VectorStore
        private set
        
    lateinit var retriever: Retriever
        private set
        
    lateinit var contextBuilder: ContextBuilder
        private set
        
    var ragEngine: RAGEngine? = null

    fun init(context: Context) {
        // Initialize singletons
        embeddingEngine = LocalEmbeddingEngine()
        vectorStore = LocalVectorStore(context) // We will update LocalVectorStore to take Context for file persistence
        retriever = DefaultRetriever(vectorStore, embeddingEngine)
        contextBuilder = DefaultContextBuilder()
    }
}
