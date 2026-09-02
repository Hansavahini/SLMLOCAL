package com.example.gemma.rag

import java.util.Locale
import kotlin.math.abs

/**
 * A lightweight, offline hashing vectorizer (TF-based) for Android.
 * Converts text into a fixed-size FloatArray without requiring a heavy ML model.
 */
class LocalEmbeddingEngine(private val dimensions: Int = 1024) : EmbeddingEngine {

    override suspend fun generateEmbedding(text: String): FloatArray {
        val vector = FloatArray(dimensions)
        
        // Tokenize by non-word characters and convert to lowercase
        val words = text.lowercase(Locale.getDefault())
            .split(Regex("\\W+"))
            .filter { it.isNotBlank() && it.length > 2 } // Ignore very short words or stop words
            
        if (words.isEmpty()) return vector
        
        // Calculate Term Frequency (TF) via hashing trick
        for (word in words) {
            val hash = abs(word.hashCode()) % dimensions
            vector[hash] += 1.0f
        }
        
        // Normalize the vector (L2 norm) so cosine similarity works correctly
        var sumSquares = 0.0f
        for (v in vector) {
            sumSquares += v * v
        }
        
        if (sumSquares > 0) {
            val magnitude = kotlin.math.sqrt(sumSquares.toDouble()).toFloat()
            for (i in vector.indices) {
                vector[i] /= magnitude
            }
        }
        
        return vector
    }
}
