package com.vishnuhs.chatapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.vishnuhs.chatapp.domain.model.User
import com.vishnuhs.chatapp.domain.repository.UsersRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UsersRepository {

    override suspend fun saveUser(user: User): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.id)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        User(
                            id = doc.id,
                            email = doc.getString("email") ?: "",
                            displayName = doc.getString("displayName") ?: "",
                            photoUrl = doc.getString("photoUrl") ?: "",
                            isOnline = doc.getBoolean("isOnline") ?: false,
                            lastSeen = doc.getLong("lastSeen") ?: System.currentTimeMillis(),
                            fcmToken = doc.getString("fcmToken") ?: "",
                            name = doc.getString("name") ?: "",
                            profileImageUrl = doc.getString("profileImageUrl")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getUserById(userId: String): User? {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                User(
                    id = doc.id,
                    email = doc.getString("email") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    isOnline = doc.getBoolean("isOnline") ?: false,
                    lastSeen = doc.getLong("lastSeen") ?: System.currentTimeMillis(),
                    fcmToken = doc.getString("fcmToken") ?: "",
                    name = doc.getString("name") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl")
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeen" to System.currentTimeMillis()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}