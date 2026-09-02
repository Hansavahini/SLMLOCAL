package com.example.gemma.repository

import com.example.gemma.db.ConversationDao
import com.example.gemma.db.ConversationEntity
import com.example.gemma.db.MessageDao
import com.example.gemma.db.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    suspend fun createConversation(title: String, activeDocumentId: String? = null, activeDocumentName: String? = null): Long {
        val time = System.currentTimeMillis()
        val entity = ConversationEntity(
            title = title,
            createdAt = time,
            updatedAt = time,
            activeDocumentId = activeDocumentId,
            activeDocumentName = activeDocumentName
        )
        return conversationDao.insertConversation(entity)
    }

    suspend fun loadConversations(): List<ConversationEntity> {
        return conversationDao.getAllConversations()
    }

    suspend fun getConversation(id: Long): ConversationEntity? {
        return conversationDao.getConversation(id)
    }

    suspend fun loadFullMessages(conversationId: Long): List<MessageEntity> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    suspend fun loadRecentMessages(conversationId: Long, limit: Int): List<MessageEntity> {
        return messageDao.getRecentMessagesForConversation(conversationId, limit)
    }

    suspend fun saveUserMessage(conversationId: Long, content: String): Long {
        val time = System.currentTimeMillis()
        val msg = MessageEntity(
            conversationId = conversationId,
            role = "USER",
            content = content,
            timestamp = time
        )
        val id = messageDao.insertMessage(msg)
        conversationDao.updateTimestamp(conversationId, time)
        return id
    }

    suspend fun saveAssistantMessage(conversationId: Long, content: String): Long {
        val time = System.currentTimeMillis()
        val msg = MessageEntity(
            conversationId = conversationId,
            role = "ASSISTANT",
            content = content,
            timestamp = time
        )
        val id = messageDao.insertMessage(msg)
        conversationDao.updateTimestamp(conversationId, time)
        return id
    }

    suspend fun deleteConversation(conversation: ConversationEntity) {
        conversationDao.deleteConversation(conversation)
    }
    
    suspend fun deleteMessagesForConversation(conversationId: Long) {
        messageDao.deleteMessagesForConversation(conversationId)
    }

    suspend fun updateTitle(conversationId: Long, title: String) {
        conversationDao.updateTitle(conversationId, title)
    }

    suspend fun updateActiveDocument(conversationId: Long, documentId: String?, documentName: String?) {
        conversationDao.updateActiveDocument(conversationId, documentId, documentName)
    }
}
