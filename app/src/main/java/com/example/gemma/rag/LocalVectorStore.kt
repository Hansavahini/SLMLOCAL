package com.example.gemma.rag

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An in-memory vector store that persists to disk and searches using Cosine Similarity.
 */
class LocalVectorStore(private val context: Context) : VectorStore {

    private data class StoredChunk(
        val chunkId: String,
        val documentId: String,
        val text: String,
        val pageNumber: Int?,
        val chapter: String?,
        val section: String?,
        val metadata: Map<String, String>,
        val embedding: FloatArray
    ) : Serializable

    private val storage = mutableListOf<StoredChunk>()
    private val storageFile by lazy { File(context.filesDir, "vector_store.dat") }
    private val mutex = Mutex()

    init {
        // Initial load should not be asynchronous to ensure object is usable immediately
        loadFromDiskSynchronously()
    }

    override suspend fun addChunk(chunk: DocumentChunk, embedding: FloatArray) {
        withContext(Dispatchers.Default) {
            mutex.withLock {
                storage.add(StoredChunk(
                    chunk.chunkId, chunk.documentId, chunk.text,
                    chunk.pageNumber, chunk.chapter, chunk.section,
                    chunk.metadata, embedding
                ))
                saveToDiskSynchronously()
            }
        }
    }

    override suspend fun search(queryEmbedding: FloatArray, documentId: String, topK: Int): List<DocumentChunk> = withContext(Dispatchers.Default) {
        mutex.withLock {
            if (storage.isEmpty()) return@withContext emptyList()

            val results = storage.filter { it.documentId == documentId }.mapIndexed { index, storedChunk ->
                if (index == 0) {
                    try {
                        android.util.Log.d("RAG_TEST", "stored_embedding_dimension=${storedChunk.embedding.size}")
                    } catch (e: Exception) {
                        // ignore in tests
                    }
                }
                val similarity = cosineSimilarity(queryEmbedding, storedChunk.embedding)
                Pair(storedChunk, similarity)
            }

            // Sort by highest similarity descending
            results.sortedByDescending { it.second }
                .take(topK)
                .map { 
                    DocumentChunk(
                        it.first.chunkId, it.first.documentId, it.first.text,
                        it.first.pageNumber, it.first.chapter, it.first.section,
                        it.first.metadata, it.second
                    )
                }
        }
    }
    
    override suspend fun hasDocument(documentId: String): Boolean = withContext(Dispatchers.Default) {
        mutex.withLock {
            storage.any { it.documentId == documentId }
        }
    }
    
    override suspend fun removeDocument(documentId: String) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val originalSize = storage.size
            storage.removeAll { it.documentId == documentId }
            if (storage.size != originalSize) {
                saveToDiskSynchronously()
            }
        }
    }
    
    override suspend fun getVectorCount(): Int = withContext(Dispatchers.Default) {
        mutex.withLock {
            storage.size
        }
    }

    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            normA += vec1[i] * vec1[i]
            normB += vec2[i] * vec2[i]
        }
        
        if (normA == 0.0f || normB == 0.0f) return 0.0f
        return (dotProduct / (kotlin.math.sqrt(normA.toDouble()) * kotlin.math.sqrt(normB.toDouble()))).toFloat()
    }

    private fun saveToDiskSynchronously() {
        try {
            ObjectOutputStream(storageFile.outputStream()).use { it.writeObject(ArrayList(storage)) }
        } catch (e: Exception) {
            android.util.Log.e("LocalVectorStore", "Failed to save vector store", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadFromDiskSynchronously() {
        if (!storageFile.exists()) return
        try {
            ObjectInputStream(storageFile.inputStream()).use {
                val loaded = it.readObject() as? ArrayList<StoredChunk>
                if (loaded != null) {
                    storage.clear()
                    storage.addAll(loaded)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LocalVectorStore", "Failed to load vector store", e)
        }
    }
}
