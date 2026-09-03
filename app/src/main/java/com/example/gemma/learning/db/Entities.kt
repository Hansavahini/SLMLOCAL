package com.example.gemma.learning.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String,
    val totalPages: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val title: String,
    val chapterNumber: Int,
    val orderIndex: Int,
    val startPage: Int,
    val endPage: Int
)

@Entity(
    tableName = "modules",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId")]
)
data class ModuleEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val title: String,
    val content: String,
    val orderIndex: Int
)

@Entity(
    tableName = "explanations",
    foreignKeys = [
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["moduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("moduleId")]
)
data class ExplanationEntity(
    @PrimaryKey val id: String,
    val moduleId: String,
    val explanation: String,
    val summary: String,
    val generatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quizzes",
    foreignKeys = [
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["moduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("moduleId")]
)
data class QuizEntity(
    @PrimaryKey val id: String,
    val moduleId: String,
    val question: String,
    val options: String, // Stored as JSON string
    val answer: String
)
