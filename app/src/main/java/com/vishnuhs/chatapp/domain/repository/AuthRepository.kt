package com.vishnuhs.chatapp.domain.repository

import com.vishnuhs.chatapp.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Flow<Flow<Boolean>>
    suspend fun signUp(email: String, password: String, name: String): Boolean
    suspend fun signOut(): Flow<Flow<Boolean>>
    suspend fun getCurrentUser(): User?
    suspend fun isUserLoggedIn(): Flow<Boolean>  // Add this method
    fun getAuthState(): Flow<Boolean>
    //suspend fun signUp(email: String, password: String): Flow<Flow<Boolean>>
}