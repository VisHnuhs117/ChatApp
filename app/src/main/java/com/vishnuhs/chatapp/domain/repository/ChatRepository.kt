package com.vishnuhs.chatapp.domain.repository

import com.vishnuhs.chatapp.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessage(message: Message): Result<Unit>
    fun getMessages(senderId: String, receiverId: String): Flow<List<Message>>
    suspend fun markMessageAsRead(messageId: String): Result<Unit>
}