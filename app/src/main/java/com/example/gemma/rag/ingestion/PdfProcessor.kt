package com.example.gemma.rag.ingestion

import android.content.Context
import android.net.Uri
import com.example.gemma.rag.DocumentChunk
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.security.MessageDigest
import java.util.UUID

class PdfProcessor(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun processPdf(uri: Uri): Flow<IngestionState> = flow {
        emit(IngestionState.Parsing(0f, "Initializing PDF reader..."))
        var document: PDDocument? = null
        try {
            // Compute SHA-256 hash of the file for a stable document ID
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hashBytes = digest.digest()
            val documentId = "doc_" + hashBytes.joinToString("") { "%02x".format(it) }
            
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                emit(IngestionState.Error("Failed to open file."))
                return@flow
            }
            
            document = PDDocument.load(inputStream)
            val pageCount = document.numberOfPages
            
            val chunks = mutableListOf<DocumentChunk>()
            val stripper = PDFTextStripper()
            
            var totalCharacters = 0
            val chapters = mutableListOf<String>()
            
            var currentChapter: String? = null
            
            // Regex to find things like "Chapter 1: The Beginning"
            val chapterRegex = Regex("(?i)^\\s*(chapter\\s+\\d+.*)\$")

            for (i in 1..pageCount) {
                emit(IngestionState.Parsing(i.toFloat() / pageCount.toFloat(), "Parsing page $i of $pageCount"))
                
                stripper.startPage = i
                stripper.endPage = i
                
                val text = stripper.getText(document).trim()
                if (text.isNotEmpty()) {
                    totalCharacters += text.length
                    
                    // Simple chapter extraction heuristic
                    val firstLine = text.lines().firstOrNull() ?: ""
                    val chapterMatch = chapterRegex.find(firstLine)
                    if (chapterMatch != null) {
                        currentChapter = chapterMatch.groupValues[1]
                        chapters.add(currentChapter)
                    }

                    chunks.add(
                        DocumentChunk(
                            chunkId = UUID.randomUUID().toString(),
                            documentId = documentId,
                            text = text,
                            pageNumber = i,
                            chapter = currentChapter,
                            section = null, // Section extraction omitted for simplicity
                            metadata = mapOf("source" to uri.toString())
                        )
                    )
                }
            }
            
            emit(IngestionState.RawCompleted(
                documentId = documentId,
                chunks = chunks,
                pageCount = pageCount,
                totalCharacters = totalCharacters,
                detectedChapters = chapters.size,
                chapterNames = chapters
            ))
            
        } catch (e: Exception) {
            emit(IngestionState.Error("Error parsing PDF: ${e.message}"))
        } finally {
            document?.close()
        }
    }.flowOn(Dispatchers.IO)
}
