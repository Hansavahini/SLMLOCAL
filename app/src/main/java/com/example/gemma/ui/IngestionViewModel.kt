package com.example.gemma.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemma.rag.RagDependencies
import com.example.gemma.rag.ingestion.IngestionState
import com.example.gemma.rag.ingestion.PdfProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class IngestionViewModel(application: Application) : AndroidViewModel(application) {

    private val pdfProcessor = PdfProcessor(application)
    private val documentChunker = com.example.gemma.rag.chunking.DocumentChunker()

    private val _ingestionState = MutableStateFlow<IngestionState>(IngestionState.Idle)
    val ingestionState: StateFlow<IngestionState> = _ingestionState.asStateFlow()

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Unknown Document"
    }

    fun startIngestion(uri: Uri) {
        val documentName = getFileName(uri)
        Log.d("RAG_TEST", "ingestion_started")
        viewModelScope.launch {
            pdfProcessor.processPdf(uri).collect { state ->
                if (state is IngestionState.RawCompleted) {
                    _ingestionState.value = IngestionState.Parsing(1f, "Chunking and Embedding text...")
                    
                    val finalChunks = documentChunker.chunk(state.chunks)
                    Log.d("RAG_TEST", "documentId=${state.documentId}, documentName=$documentName")
                    Log.d("RAG_TEST", "chunks=${finalChunks.size}")
                    
                    // Generate embeddings and store
                    val embeddingEngine = RagDependencies.embeddingEngine
                    val vectorStore = RagDependencies.vectorStore
                    
                    if (!vectorStore.hasDocument(state.documentId)) {
                        for ((index, chunk) in finalChunks.withIndex()) {
                            _ingestionState.value = IngestionState.Parsing(1f, "Embedding chunk ${index + 1}/${finalChunks.size}...")
                            val embedding = embeddingEngine.generateEmbedding(chunk.text)
                            vectorStore.addChunk(chunk, embedding)
                        }
                        Log.d("RAG_TEST", "embeddings_generated=${finalChunks.size}")
                    } else {
                        _ingestionState.value = IngestionState.Parsing(1f, "Document already indexed, skipping embedding...")
                        kotlinx.coroutines.delay(500)
                    }
                    
                    Log.d("RAG_TEST", "vector_store_count=${vectorStore.getVectorCount()}")
                    
                    val sizes = finalChunks.map { it.text.length }
                    val avgSize = if (sizes.isNotEmpty()) sizes.average().toInt() else 0
                    val minSize = sizes.minOrNull() ?: 0
                    val maxSize = sizes.maxOrNull() ?: 0
                    
                    _ingestionState.value = IngestionState.Completed(
                        documentId = state.documentId,
                        documentName = documentName,
                        pageCount = state.pageCount,
                        chunkCount = finalChunks.size,
                        totalCharacters = state.totalCharacters,
                        detectedChapters = state.detectedChapters,
                        averageChunkSize = avgSize,
                        minChunkSize = minSize,
                        maxChunkSize = maxSize,
                        averageOverlap = 200, // Approximate for now
                        firstPageSample = finalChunks.firstOrNull()?.text?.take(200) ?: "",
                        middlePageSample = finalChunks.getOrNull(finalChunks.size / 2)?.text?.take(200) ?: "",
                        lastPageSample = finalChunks.lastOrNull()?.text?.take(200) ?: "",
                        chapterNames = state.chapterNames
                    )
                } else {
                    _ingestionState.value = state
                }
            }
        }
    }

    fun resetState() {
        _ingestionState.value = IngestionState.Idle
    }
}
