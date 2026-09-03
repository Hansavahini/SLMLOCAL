package com.example.gemma.learning.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: String): DocumentEntity?
}

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM chapters WHERE documentId = :documentId ORDER BY orderIndex ASC")
    fun getChaptersForDocument(documentId: String): Flow<List<ChapterEntity>>
}

@Dao
interface ModuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<ModuleEntity>)

    @Query("SELECT * FROM modules WHERE chapterId = :chapterId ORDER BY orderIndex ASC")
    fun getModulesForChapter(chapterId: String): Flow<List<ModuleEntity>>
    
    @Query("SELECT * FROM modules WHERE id = :moduleId")
    suspend fun getModuleById(moduleId: String): ModuleEntity?
}

@Dao
interface ExplanationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExplanation(explanation: ExplanationEntity)

    @Query("SELECT * FROM explanations WHERE moduleId = :moduleId")
    suspend fun getExplanationForModule(moduleId: String): ExplanationEntity?
}

@Dao
interface QuizDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizzes(quizzes: List<QuizEntity>)

    @Query("SELECT * FROM quizzes WHERE moduleId = :moduleId")
    fun getQuizzesForModule(moduleId: String): Flow<List<QuizEntity>>
}
