package com.example.gemma.learning.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemma.ai.LLMEngine
import com.example.gemma.learning.LearningRepository
import com.example.gemma.learning.db.ChapterEntity
import com.example.gemma.learning.db.DocumentEntity
import com.example.gemma.learning.db.LearningDatabase
import com.example.gemma.learning.db.ModuleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = LearningDatabase.getDatabase(application)
    private val repository = LearningRepository(database)

    private val _documents = MutableStateFlow<List<DocumentEntity>>(emptyList())
    val documents: StateFlow<List<DocumentEntity>> = _documents.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val chapters: StateFlow<List<ChapterEntity>> = _chapters.asStateFlow()

    private val _modules = MutableStateFlow<List<ModuleEntity>>(emptyList())
    val modules: StateFlow<List<ModuleEntity>> = _modules.asStateFlow()

    private val _explanationStream = MutableStateFlow("")
    val explanationStream: StateFlow<String> = _explanationStream.asStateFlow()

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            database.documentDao().getAllDocuments().collect { docs ->
                _documents.value = docs
            }
        }
    }

    fun loadChapters(documentId: String) {
        viewModelScope.launch {
            database.chapterDao().getChaptersForDocument(documentId).collect { chaps ->
                _chapters.value = chaps
            }
        }
    }

    fun loadModules(chapterId: String) {
        viewModelScope.launch {
            database.moduleDao().getModulesForChapter(chapterId).collect { mods ->
                _modules.value = mods
            }
        }
    }

    fun generateExplanation(moduleId: String, llmEngine: LLMEngine) {
        viewModelScope.launch {
            _explanationStream.value = "" // Reset
            repository.getExplanationStream(moduleId, llmEngine).collect { token ->
                if (token.startsWith("Error:")) {
                    _explanationStream.value = token
                } else {
                    _explanationStream.value += token
                }
            }
        }
    }
}
