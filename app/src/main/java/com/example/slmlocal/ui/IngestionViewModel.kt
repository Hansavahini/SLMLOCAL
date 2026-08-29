package com.example.slmlocal.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.slmlocal.rag.ingestion.IngestionState
import com.example.slmlocal.rag.ingestion.PdfProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IngestionViewModel(application: Application) : AndroidViewModel(application) {

    private val pdfProcessor = PdfProcessor(application)
    private val documentChunker = com.example.slmlocal.rag.chunking.DocumentChunker()

    private val _ingestionState = MutableStateFlow<IngestionState>(IngestionState.Idle)
    val ingestionState: StateFlow<IngestionState> = _ingestionState.asStateFlow()

    fun startIngestion(uri: Uri) {
        viewModelScope.launch {
            pdfProcessor.processPdf(uri).collect { state ->
                if (state is IngestionState.RawCompleted) {
                    _ingestionState.value = IngestionState.Parsing(1f, "Chunking extracted text...")
                    
                    val finalChunks = documentChunker.chunk(state.chunks)
                    
                    val sizes = finalChunks.map { it.text.length }
                    val avgSize = if (sizes.isNotEmpty()) sizes.average().toInt() else 0
                    val minSize = sizes.minOrNull() ?: 0
                    val maxSize = sizes.maxOrNull() ?: 0
                    
                    _ingestionState.value = IngestionState.Completed(
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
