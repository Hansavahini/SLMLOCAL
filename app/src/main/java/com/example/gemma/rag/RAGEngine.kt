package com.example.gemma.rag

import kotlinx.coroutines.flow.Flow

interface RAGEngine {
    fun answerQuestionStream(question: String, documentId: String): Flow<String>
    suspend fun summarizeDocument(documentId: String): String
    suspend fun summarizeChapter(chapterName: String): String
}
