package com.example.slmlocal.rag

import kotlinx.coroutines.flow.Flow

interface RAGEngine {
    fun answerQuestionStream(question: String): Flow<String>
    suspend fun summarizeDocument(documentId: String): String
    suspend fun summarizeChapter(chapterName: String): String
}
