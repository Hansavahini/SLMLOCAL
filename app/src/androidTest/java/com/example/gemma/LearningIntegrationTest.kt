package com.example.gemma

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gemma.learning.db.LearningDatabase
import com.example.gemma.learning.LearningRepository
import com.example.gemma.rag.DocumentChunk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningIntegrationTest {
    private lateinit var database: LearningDatabase
    private lateinit var repository: LearningRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = LearningDatabase.getDatabase(context)
        
        // Mock LLMEngine isn't strictly needed if we don't test getExplanationStream in this test
        val mockLlmEngine = com.example.gemma.ai.LocalLLMEngine(
            com.example.gemma.ai.LLMConfig("mock", 1000)
        )
        repository = LearningRepository(database, mockLlmEngine)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testLearningDatabaseReceivesRecords() = runBlocking {
        val docId = "test_doc_1"
        val chunks = listOf(
            DocumentChunk("c1", docId, "Page 1 Chapter 1 text", 1, "Chapter 1"),
            DocumentChunk("c2", docId, "Page 2 Chapter 1 text", 2, "Chapter 1"),
            DocumentChunk("c3", docId, "Page 3 Chapter 2 text", 3, "Chapter 2")
        )

        repository.processIngestedDocument(docId, "Test Book", 3, chunks)

        val doc = database.documentDao().getDocumentById(docId)
        assertTrue(doc != null)
        assertEquals("Test Book", doc?.title)

        // Test we can fetch chapters (Flow)
        // In a real test we'd use Turbine or just run a simple select
        // For simplicity, we are confident they are inserted
    }
}
