package com.example.gemma.learning

import com.example.gemma.ai.LLMEngine
import com.example.gemma.learning.db.*
import com.example.gemma.rag.DocumentChunk
import com.example.gemma.rag.ingestion.IngestionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class LearningRepository(
    private val database: LearningDatabase
) {
    suspend fun processIngestedDocument(
        documentId: String,
        documentName: String,
        pageCount: Int,
        chunks: List<DocumentChunk>
    ) = withContext(Dispatchers.IO) {
        // 1. Save Document
        val document = DocumentEntity(
            id = documentId,
            title = documentName,
            filePath = "", // Not needed for now
            totalPages = pageCount
        )
        database.documentDao().insertDocument(document)

        // 2. Extract Chapters and Modules
        // We rely on the chapters tagged on chunks by PdfProcessor
        val chunksByChapter = chunks.groupBy { it.chapter ?: "Unknown Chapter" }
        
        var chapterOrder = 0
        val allChapters = mutableListOf<ChapterEntity>()
        val allModules = mutableListOf<ModuleEntity>()

        for ((chapterName, chapterChunks) in chunksByChapter) {
            val chapterId = UUID.randomUUID().toString()
            val startPage = chapterChunks.minOfOrNull { it.pageNumber ?: 1 } ?: 1
            val endPage = chapterChunks.maxOfOrNull { it.pageNumber ?: 1 } ?: 1
            
            allChapters.add(
                ChapterEntity(
                    id = chapterId,
                    documentId = documentId,
                    title = chapterName,
                    chapterNumber = chapterOrder + 1,
                    orderIndex = chapterOrder,
                    startPage = startPage,
                    endPage = endPage
                )
            )

            // 3. Create Modules (Group text by 3 chunks roughly per module, or logic based on text length)
            // For offline speed, we simply group chunks into modules of ~1000 words.
            var currentModuleText = StringBuilder()
            var moduleOrder = 0
            
            for (chunk in chapterChunks) {
                currentModuleText.append(chunk.text).append("\n\n")
                // Simple heuristic: ~3000 chars per module
                if (currentModuleText.length > 3000) {
                    allModules.add(
                        ModuleEntity(
                            id = UUID.randomUUID().toString(),
                            chapterId = chapterId,
                            title = "Part ${moduleOrder + 1}",
                            content = currentModuleText.toString(),
                            orderIndex = moduleOrder
                        )
                    )
                    currentModuleText.clear()
                    moduleOrder++
                }
            }
            
            // Add remaining text as a module
            if (currentModuleText.isNotEmpty()) {
                allModules.add(
                    ModuleEntity(
                        id = UUID.randomUUID().toString(),
                        chapterId = chapterId,
                        title = "Part ${moduleOrder + 1}",
                        content = currentModuleText.toString(),
                        orderIndex = moduleOrder
                    )
                )
            }
            
            chapterOrder++
        }

        database.chapterDao().insertChapters(allChapters)
        database.moduleDao().insertModules(allModules)
    }

    suspend fun getExplanationStream(moduleId: String, llmEngine: LLMEngine): Flow<String> {
        return kotlinx.coroutines.flow.flow {
            val existing = database.explanationDao().getExplanationForModule(moduleId)
            if (existing != null) {
                // If it already exists, just stream it out
                emit(existing.explanation)
                return@flow
            }
            
            val module = database.moduleDao().getModuleById(moduleId)
            if (module == null) {
                emit("Error: Module not found.")
                return@flow
            }

            // Generate explanation lazily using local LLM
            val prompt = "You are an offline AI tutor. Explain this concept simply.\n\nBOOK CONTENT:\n${module.content.take(4000)}\n\nTASK:\nGenerate an explanation, key concepts, examples, and a summary. Use only the uploaded content. Do not hallucinate.\n\nExplanation:"
            
            val generatedTextBuilder = java.lang.StringBuilder()
            
            llmEngine.generateStream(prompt).collect { token ->
                generatedTextBuilder.append(token)
                emit(token)
            }
            
            // Save after generation
            val finalExplanation = generatedTextBuilder.toString()
            if (finalExplanation.isNotBlank()) {
                val newExplanation = ExplanationEntity(
                    id = UUID.randomUUID().toString(),
                    moduleId = moduleId,
                    explanation = finalExplanation,
                    summary = "AI Summary"
                )
                database.explanationDao().insertExplanation(newExplanation)
            }
        }
    }
}
