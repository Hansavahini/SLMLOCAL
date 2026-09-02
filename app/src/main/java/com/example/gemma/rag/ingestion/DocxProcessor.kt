package com.example.gemma.rag.ingestion

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream

class DocxProcessor {

    companion object {
        private const val TAG = "DocxProcessor"
    }

    suspend fun extractText(inputStream: InputStream): String = withContext(Dispatchers.IO) {
        val stringBuilder = StringBuilder()
        
        try {
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val xmlContent = zis.bufferedReader().readText()
                        
                        // Replace paragraph ends with newlines to preserve structure
                        val spacedContent = xmlContent.replace(Regex("</w:p>"), "\n")
                        
                        // Extract text from <w:t> tags
                        val regex = Regex("<w:t[^>]*>(.*?)</w:t>")
                        val matches = regex.findAll(spacedContent)
                        
                        for (match in matches) {
                            stringBuilder.append(match.groupValues[1])
                        }
                        
                        // Basic formatting - could be improved by looking at <w:p> tags for paragraphs
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse DOCX file", e)
        }
        
        stringBuilder.toString()
    }
}
