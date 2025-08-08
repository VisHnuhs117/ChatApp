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
            val messageData = hashMapOf(
                "senderId" to message.senderId,
                "receiverId" to message.receiverId,
                "content" to message.content,
                "timestamp" to message.timestamp,
                "senderName" to message.senderName,
                "isRead" to message.isRead
            )

            firestore.collection("messages")
                .add(messageData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG: Error sending message: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getMessages(senderId: String, receiverId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("messages")
            .whereIn("senderId", listOf(senderId, receiverId))
            .whereIn("receiverId", listOf(senderId, receiverId))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("DEBUG: Firestore listener error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val msg = Message(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            receiverId = doc.getString("receiverId") ?: "",
                            content = doc.getString("content") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            senderName = doc.getString("senderName") ?: "",
                            isRead = doc.getBoolean("isRead") ?: false
                        )

                        // Only include messages between these two users
                        if ((msg.senderId == senderId && msg.receiverId == receiverId) ||
                            (msg.senderId == receiverId && msg.receiverId == senderId)) {
                            msg
                        } else null
                    } catch (e: Exception) {
                        println("DEBUG: Error parsing message: ${e.message}")
                        null
                    }
                } ?: emptyList()

                println("DEBUG: Loaded ${messages.size} messages")
                trySend(messages)
            }

        awaitClose {
            println("DEBUG: Closing messages listener")
            listener.remove()
        }
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