package com.example.slmlocal.rag.chunking

import com.example.slmlocal.rag.DocumentChunk
import java.util.UUID

class DocumentChunker(private val config: ChunkingConfig = ChunkingConfig()) {

    fun chunk(pages: List<DocumentChunk>): List<DocumentChunk> {
        val finalChunks = mutableListOf<DocumentChunk>()
        if (pages.isEmpty()) return finalChunks
        
        val documentId = pages.first().documentId
        
        // Flatten text while keeping track of page boundaries
        val textBlocks = mutableListOf<TextBlock>()
        for (page in pages) {
            val text = page.text.trim()
            if (text.isNotEmpty()) {
                textBlocks.add(TextBlock(text, page.pageNumber, page.chapter, page.section))
            }
        }
        
        if (textBlocks.isEmpty()) return finalChunks
        
        // We'll iterate through all paragraphs/sentences, trying to group them into targetChunkSize
        val allTextParts = mutableListOf<TextPart>()
        
        for (block in textBlocks) {
            val paragraphs = splitIntoParagraphs(block.text)
            for (paragraph in paragraphs) {
                if (paragraph.length > config.maxChunkSize) {
                    // Split into sentences if paragraph is too large
                    val sentences = splitIntoSentences(paragraph)
                    for (sentence in sentences) {
                        if (sentence.length > config.maxChunkSize) {
                            // As a last resort, split by spaces or just arbitrarily
                            val parts = sentence.chunked(config.targetChunkSize)
                            for (part in parts) {
                                allTextParts.add(TextPart(part, block))
                            }
                        } else {
                            allTextParts.add(TextPart(sentence, block))
                        }
                    }
                } else {
                    allTextParts.add(TextPart(paragraph, block))
                }
            }
        }
        
        var currentChunkText = StringBuilder()
        var currentStartPage: Int? = null
        var currentEndPage: Int? = null
        var currentChapter: String? = null
        var currentSection: String? = null
        
        fun buildAndAddChunk(overlapParts: List<TextPart>) {
            if (currentChunkText.length >= config.minChunkSize) {
                val metadata = mutableMapOf<String, String>()
                if (currentStartPage != null) metadata["startPage"] = currentStartPage.toString()
                if (currentEndPage != null) metadata["endPage"] = currentEndPage.toString()
                
                finalChunks.add(DocumentChunk(
                    chunkId = UUID.randomUUID().toString(),
                    documentId = documentId,
                    text = currentChunkText.toString().trim(),
                    pageNumber = if (currentStartPage == currentEndPage) currentStartPage else null,
                    chapter = currentChapter,
                    section = currentSection,
                    metadata = metadata
                ))
            }
            currentChunkText.clear()
            
            // Apply overlap if we have it
            var overlapText = StringBuilder()
            // Take from the end of overlapParts to fill up to config.overlapSize
            var overlapSizeSoFar = 0
            for (i in overlapParts.indices.reversed()) {
                val part = overlapParts[i]
                if (overlapSizeSoFar + part.text.length <= config.overlapSize * 1.5) { // allow slightly more for paragraph completeness
                    overlapText.insert(0, part.text + if(part.text.endsWith("\n")) "" else " ")
                    overlapSizeSoFar += part.text.length
                    
                    // Reset metadata trackers for the new chunk starting with overlap
                    if (currentStartPage == null || (part.block.pageNumber != null && part.block.pageNumber < currentStartPage!!)) {
                        currentStartPage = part.block.pageNumber
                    }
                    if (currentChapter == null) currentChapter = part.block.chapter
                    if (currentSection == null) currentSection = part.block.section
                } else {
                    break
                }
            }
            if (overlapText.isNotEmpty()) {
                currentChunkText.append(overlapText.toString().trim())
                currentChunkText.append("\n\n")
            }
        }
        
        var historyForOverlap = mutableListOf<TextPart>()
        
        for (part in allTextParts) {
            val potentialSize = currentChunkText.length + part.text.length
            
            if (potentialSize > config.maxChunkSize && currentChunkText.isNotEmpty()) {
                // Time to finish current chunk
                buildAndAddChunk(historyForOverlap)
                historyForOverlap.clear()
            }
            
            // Append part to current chunk
            if (currentChunkText.isNotEmpty()) {
                if (!currentChunkText.endsWith("\n") && !currentChunkText.endsWith(" ")) {
                     currentChunkText.append(" ")
                }
            }
            
            if (currentStartPage == null) currentStartPage = part.block.pageNumber
            currentEndPage = part.block.pageNumber
            if (currentChapter == null) currentChapter = part.block.chapter
            if (currentSection == null) currentSection = part.block.section
            
            currentChunkText.append(part.text)
            historyForOverlap.add(part)
        }
        
        // Add final chunk
        if (currentChunkText.isNotEmpty()) {
            buildAndAddChunk(emptyList()) // No overlap needed for the end
        }
        
        return finalChunks
    }
    
    private fun splitIntoParagraphs(text: String): List<String> {
        return text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
    }
    
    private fun splitIntoSentences(text: String): List<String> {
        // Improved sentence splitting avoiding abbreviations like "Dr.", "e.g.", "i.e."
        val sentenceBoundary = Regex("(?<=[.!?])\\s+(?=[A-Z])")
        return text.split(sentenceBoundary).map { it.trim() }.filter { it.isNotEmpty() }
    }
    
    private data class TextBlock(val text: String, val pageNumber: Int?, val chapter: String?, val section: String?)
    private data class TextPart(val text: String, val block: TextBlock)
}
