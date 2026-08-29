package com.example.slmlocal.rag.ingestion

sealed class IngestionState {
    object Idle : IngestionState()
    data class Parsing(val progress: Float, val currentActivity: String) : IngestionState()
    data class Completed(
        val pageCount: Int,
        val chunkCount: Int,
        val totalCharacters: Int,
        val detectedChapters: Int,
        val averageChunkSize: Int,
        val minChunkSize: Int,
        val maxChunkSize: Int,
        val averageOverlap: Int,
        val firstPageSample: String,
        val middlePageSample: String,
        val lastPageSample: String,
        val chapterNames: List<String>
    ) : IngestionState()
    
    data class RawCompleted(
        val chunks: List<com.example.slmlocal.rag.DocumentChunk>,
        val pageCount: Int,
        val totalCharacters: Int,
        val detectedChapters: Int,
        val chapterNames: List<String>
    ) : IngestionState()
    
    data class Error(val message: String) : IngestionState()
}
