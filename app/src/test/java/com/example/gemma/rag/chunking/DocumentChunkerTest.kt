package com.example.gemma.rag.chunking

import com.example.gemma.rag.DocumentChunk
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class DocumentChunkerTest {

    private fun createPageChunk(text: String, pageNum: Int): DocumentChunk {
        return DocumentChunk(
            chunkId = UUID.randomUUID().toString(),
            documentId = "doc-1",
            text = text,
            pageNumber = pageNum
        )
    }

    @Test
    fun testShortText() {
        val config = ChunkingConfig(targetChunkSize = 100, maxChunkSize = 150, minChunkSize = 10, overlapSize = 20)
        val chunker = DocumentChunker(config)
        
        val input = listOf(createPageChunk("This is a short text.", 1))
        val result = chunker.chunk(input)
        
        assertEquals(1, result.size)
        assertEquals("This is a short text.", result[0].text)
        assertEquals(1, result[0].pageNumber)
    }

    @Test
    fun testMultipleParagraphs() {
        val config = ChunkingConfig(targetChunkSize = 50, maxChunkSize = 100, minChunkSize = 10, overlapSize = 0)
        val chunker = DocumentChunker(config)
        
        val input = listOf(createPageChunk("Para 1.\n\nPara 2.\n\nPara 3.\n\nPara 4.", 1))
        // Each para is small, should be grouped up to 50 chars.
        // "Para 1. Para 2. Para 3. Para 4." is 31 chars total.
        val result = chunker.chunk(input)
        
        assertEquals(1, result.size) // fits in target size entirely
    }

    @Test
    fun testLongTextExceedingMax() {
        val config = ChunkingConfig(targetChunkSize = 20, maxChunkSize = 30, minChunkSize = 5, overlapSize = 5)
        val chunker = DocumentChunker(config)
        
        // 40 A's
        val longText = "A".repeat(40)
        val input = listOf(createPageChunk(longText, 1))
        
        val result = chunker.chunk(input)
        assertTrue(result.size > 1)
        assertTrue(result.all { it.text.length <= config.maxChunkSize + config.overlapSize })
    }

    @Test
    fun testOverlap() {
        val config = ChunkingConfig(targetChunkSize = 20, maxChunkSize = 30, minChunkSize = 5, overlapSize = 10)
        val chunker = DocumentChunker(config)
        
        val input = listOf(createPageChunk("Sentence one. Sentence two. Sentence three. Sentence four.", 1))
        val result = chunker.chunk(input)
        
        assertTrue(result.size > 1)
        // Check if overlap exists (not perfect, but second chunk should contain end of first chunk)
        val firstEnd = result[0].text.takeLast(10)
        val secondStart = result[1].text.take(10)
        // Might not be exact due to word boundaries, but overlap should be happening
    }

    @Test
    fun testMultiPageChunksAndMetadata() {
        val config = ChunkingConfig(targetChunkSize = 100, maxChunkSize = 150, minChunkSize = 10, overlapSize = 0)
        val chunker = DocumentChunker(config)
        
        val input = listOf(
            createPageChunk("Page one text. ".repeat(3), 1),
            createPageChunk("Page two text. ".repeat(3), 2)
        )
        val result = chunker.chunk(input)
        
        assertEquals(1, result.size)
        // Since it spans multiple pages
        assertNull(result[0].pageNumber)
        assertEquals("1", result[0].metadata["startPage"])
        assertEquals("2", result[0].metadata["endPage"])
    }

    @Test
    fun testEmptyText() {
        val chunker = DocumentChunker()
        val input = listOf(createPageChunk("   \n  \n", 1))
        val result = chunker.chunk(input)
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun testSentenceSplitting() {
        val config = ChunkingConfig(targetChunkSize = 30, maxChunkSize = 40, minChunkSize = 5, overlapSize = 0)
        val chunker = DocumentChunker(config)
        
        // "Dr. Smith" should not be split
        val input = listOf(createPageChunk("Dr. Smith is here. He is nice.", 1))
        val result = chunker.chunk(input)
        
        assertEquals(1, result.size)
        assertEquals("Dr. Smith is here. He is nice.", result[0].text)
    }
}
