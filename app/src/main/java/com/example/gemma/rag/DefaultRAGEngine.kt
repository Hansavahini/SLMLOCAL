package com.example.gemma.rag

import com.example.gemma.ai.LLMEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import android.util.Log

class DefaultRAGEngine(
    private val llmEngine: LLMEngine,
    private val retriever: Retriever,
    private val contextBuilder: ContextBuilder,
    private val relevanceThreshold: Float = -1.0f // COMPLETELY DISABLED THRESHOLD
) : RAGEngine {

    private fun safeLog(msg: String) {
        try {
            Log.d("RAG_TEST", msg)
        } catch (e: Exception) {
            // ignore in tests
        }
    }

    override fun answerQuestionStream(question: String, documentId: String): Flow<String> = kotlinx.coroutines.flow.flow {
        safeLog("retrieval_started documentId=$documentId")
        // Retrieve relevant context chunks
        val contextChunks = retriever.retrieveContext(question, documentId, maxChunks = 3)
        
        val topScore = contextChunks.firstOrNull()?.score ?: 0f
        val allScores = contextChunks.map { it.score }
        
        val relevantChunks = contextChunks.filter { it.score >= relevanceThreshold }
        val llmCalled = relevantChunks.isNotEmpty()
        val groundingDecision = if (llmCalled) "ACCEPTED" else "REJECTED"
        
        safeLog("query=\"$question\", topScore=$topScore, allScores=$allScores, retrieved=${contextChunks.size}, relevant=${relevantChunks.size}, threshold=$relevanceThreshold, llmCalled=$llmCalled, groundingDecision=$groundingDecision")
        
        for (chunk in contextChunks) {
            val chunkSnippet = chunk.text.replace("\n", " ").take(200)
            val accepted = if (chunk.score >= relevanceThreshold) "YES" else "NO"
            safeLog("retrieved chunkId=${chunk.chunkId}, documentId=${chunk.documentId}, textLen=${chunk.text.length}, page=${chunk.pageNumber ?: chunk.metadata["startPage"] ?: "N/A"}, chapter=${chunk.chapter ?: "N/A"}, score=${chunk.score}, accepted=$accepted, snippet=\"$chunkSnippet...\"")
        }
        
        if (!llmCalled) {
            // Strict gating: No context found above threshold for this document
            val msg = "I couldn't find this information in the uploaded document."
            var first = true
            for (char in msg) {
                if (first) {
                    safeLog("answer_stream_started (not found)")
                    first = false
                }
                emit(char.toString())
                kotlinx.coroutines.delay(10)
            }
            return@flow
        }
        
        // Build the augmented prompt
        val contextText = contextBuilder.buildPrompt(question, relevantChunks)
        val contextSnippet = contextText.replace("\n", " ").take(500)
        safeLog("context_built totalLen=${contextText.length}, chunkCount=${relevantChunks.size}, snippet=\"$contextSnippet...\"")
        
        val prompt = "Answer the question based ONLY on the following context. If the answer cannot be found in the context, say so.\n\nContext:\n$contextText\n\nQuestion: $question\nAnswer:"
        
        val promptSnippet = prompt.replace("\n", " ").take(1000)
        safeLog("rag_prompt_sent promptLen=${prompt.length}, context_starts_at_index=${prompt.indexOf("Context:\n") + 9}, context_ends_at_index=${prompt.indexOf("\n\nQuestion:")}, snippet=\"$promptSnippet...\"")
        // Stream the answer from the LLM Engine
        var firstToken = true
        llmEngine.generateStream(prompt).collect { token ->
            if (firstToken) {
                safeLog("answer_stream_started")
                firstToken = false
            }
            emit(token)
        }
    }

    override suspend fun summarizeDocument(documentId: String): String {
        return "Summary of document"
    }

    override suspend fun summarizeChapter(chapterName: String): String {
        return "Summary of chapter"
    }
}
