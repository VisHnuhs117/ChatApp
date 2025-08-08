package com.vishnuhs.chatapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vishnuhs.chatapp.domain.model.User
import com.vishnuhs.chatapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Flow<Flow<Boolean>> = flow {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val isSuccess = result.user != null

            // Update user online status when they sign in
            if (isSuccess && result.user != null) {
                try {
                    firestore.collection("users")
                        .document(result.user!!.uid)
                        .update(
                            mapOf(
                                "isOnline" to true,
                                "lastSeen" to System.currentTimeMillis()
                            )
                        )
                        .await()
                } catch (e: Exception) {
                    // Continue even if status update fails
                }
            }

            emit(flow { emit(isSuccess) })
        } catch (e: Exception) {
            emit(flow { emit(false) })
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                // Create user document in Firestore
                val userData = mapOf(
                    "id" to user.uid,
                    "email" to email,
                    "displayName" to name,
                    "name" to name,
                    "photoUrl" to "",
                    "isOnline" to true,
                    "lastSeen" to System.currentTimeMillis(),
                    "fcmToken" to "",
                    "profileImageUrl" to null
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .set(userData)
                    .await()

                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun signOut(): Flow<Flow<Boolean>> = flow {
        try {
            // Update user online status before signing out
            val currentUser = auth.currentUser
            if (currentUser != null) {
                try {
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .update(
                            mapOf(
                                "isOnline" to false,
                                "lastSeen" to System.currentTimeMillis()
                            )
                        )
                        .await()
                } catch (e: Exception) {
                    // Continue with sign out even if status update fails
                }
            }

            auth.signOut()
            emit(flow { emit(true) })
        } catch (e: Exception) {
            emit(flow { emit(false) })
        }
    }

    override suspend fun getCurrentUser(): User? {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            User(
                id = currentUser.uid,
                email = currentUser.email ?: "",
                displayName = currentUser.displayName ?: "User",
                photoUrl = currentUser.photoUrl?.toString() ?: "",
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                fcmToken = "",
                name = currentUser.displayName ?: "User",
                profileImageUrl = currentUser.photoUrl?.toString()
            )
        } else null
    }

    override suspend fun isUserLoggedIn(): Flow<Boolean> = flow {
        emit(auth.currentUser != null)
    }

    override fun getAuthState(): Flow<Boolean> = flow {
        emit(auth.currentUser != null)
    }
}