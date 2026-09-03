package com.example.gemma.learning.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DocumentEntity::class,
        ChapterEntity::class,
        ModuleEntity::class,
        ExplanationEntity::class,
        QuizEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun chapterDao(): ChapterDao
    abstract fun moduleDao(): ModuleDao
    abstract fun explanationDao(): ExplanationDao
    abstract fun quizDao(): QuizDao

    companion object {
        @Volatile
        private var INSTANCE: LearningDatabase? = null

        fun getDatabase(context: Context): LearningDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LearningDatabase::class.java,
                    "learning_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
