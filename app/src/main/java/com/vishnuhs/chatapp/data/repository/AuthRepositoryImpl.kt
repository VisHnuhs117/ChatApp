package com.vishnuhs.chatapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.vishnuhs.chatapp.domain.model.User
import com.vishnuhs.chatapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Flow<Flow<Boolean>> = flow {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val isSuccess = result.user != null
            emit(flow { emit(isSuccess) })
        } catch (e: Exception) {
            emit(flow { emit(false) })
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun signOut(): Flow<Flow<Boolean>> = flow {
        try {
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