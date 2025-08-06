package com.vishnuhs.chatapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vishnuhs.chatapp.domain.model.Message
import com.vishnuhs.chatapp.domain.repository.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    override suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            firestore.collection("messages")
                .add(message)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getMessages(senderId: String, receiverId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("messages")
            .where(
                com.google.firebase.firestore.Filter.or(
                    com.google.firebase.firestore.Filter.and(
                        com.google.firebase.firestore.Filter.equalTo("senderId", senderId),
                        com.google.firebase.firestore.Filter.equalTo("receiverId", receiverId)
                    ),
                    com.google.firebase.firestore.Filter.and(
                        com.google.firebase.firestore.Filter.equalTo("senderId", receiverId),
                        com.google.firebase.firestore.Filter.equalTo("receiverId", senderId)
                    )
                )
            )
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Message(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            receiverId = doc.getString("receiverId") ?: "",
                            content = doc.getString("content") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            senderName = doc.getString("senderName") ?: "",
                            isRead = doc.getBoolean("isRead") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        return try {
            firestore.collection("messages")
                .document(messageId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}