package com.example.slmlocal.rag

import com.example.slmlocal.ai.LLMEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class DefaultRAGEngine(
    private val llmEngine: LLMEngine,
    private val retriever: Retriever,
    private val contextBuilder: ContextBuilder
) : RAGEngine {

    override fun answerQuestionStream(question: String): Flow<String> {
        // Stub implementation for now
        return emptyFlow()
    }

    override suspend fun summarizeDocument(documentId: String): String {
        // Stub implementation
        return "Summary of document"
    }

    override suspend fun summarizeChapter(chapterName: String): String {
        // Stub implementation
        return "Summary of chapter"
    }
}
