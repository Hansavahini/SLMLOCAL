package com.example.slmlocal

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.slmlocal.rag.ingestion.PdfProcessor
import com.example.slmlocal.rag.ingestion.IngestionState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class IngestionTest {

    @Test
    fun testPdfIngestion() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pdfProcessor = PdfProcessor(context)
        
        // Use a file path for the test, converted to Uri
        val file = File("/sdcard/Download/book.pdf")
        val uri = Uri.fromFile(file)

        val states = pdfProcessor.processPdf(uri).toList()
        
        val errorState = states.filterIsInstance<IngestionState.Error>().firstOrNull()

        if (errorState != null) {
            println("INGESTION_RESULT_ERROR: ${errorState.message}")
        }
        
        val rawState = states.filterIsInstance<IngestionState.RawCompleted>().firstOrNull()
        
        if (rawState != null) {
            val chunker = com.example.slmlocal.rag.chunking.DocumentChunker()
            val finalChunks = chunker.chunk(rawState.chunks)
            
            val sizes = finalChunks.map { it.text.length }
            val avgSize = if (sizes.isNotEmpty()) sizes.average().toInt() else 0
            val minSize = sizes.minOrNull() ?: 0
            val maxSize = sizes.maxOrNull() ?: 0
            
            println("--- CHUNKING STATISTICS ---")
            println("Source page count: ${rawState.pageCount}")
            println("Pages containing text: ${rawState.chunks.size}")
            println("Pages without text: ${rawState.pageCount - rawState.chunks.size}")
            println("Total extracted characters: ${rawState.totalCharacters}")
            println("Final chunk count: ${finalChunks.size}")
            println("Average chunk size: $avgSize chars")
            println("Minimum chunk size: $minSize chars")
            println("Maximum chunk size: $maxSize chars")
            println("Average overlap: ~200 chars") // Hardcoded estimate based on config
            
            println("\n--- FIRST 3 CHUNKS ---")
            finalChunks.take(3).forEachIndexed { i, c ->
                println("Chunk ${i+1} [ID: ${c.chunkId}] [Page: ${c.pageNumber ?: "${c.metadata["startPage"]}-${c.metadata["endPage"]}"}]")
                println(c.text.take(150).replace("\n", " ") + "...")
                println("-")
            }
            
            println("\n--- MIDDLE CHUNKS ---")
            val midIdx = finalChunks.size / 2
            finalChunks.drop(midIdx).take(2).forEachIndexed { i, c ->
                println("Chunk ${midIdx + i + 1} [ID: ${c.chunkId}] [Page: ${c.pageNumber ?: "${c.metadata["startPage"]}-${c.metadata["endPage"]}"}]")
                println(c.text.take(150).replace("\n", " ") + "...")
                println("-")
            }
            
            println("\n--- LAST 3 CHUNKS ---")
            finalChunks.takeLast(3).forEachIndexed { i, c ->
                println("Chunk ${finalChunks.size - 3 + i + 1} [ID: ${c.chunkId}] [Page: ${c.pageNumber ?: "${c.metadata["startPage"]}-${c.metadata["endPage"]}"}]")
                println(c.text.take(150).replace("\n", " ") + "...")
                println("-")
            }
        } else {
            println("INGESTION_RESULT_NO_COMPLETION")
        }
    }
}
