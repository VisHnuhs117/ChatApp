package com.vishnuhs.chatapp.domain.repository

import com.vishnuhs.chatapp.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UsersRepository {
    suspend fun saveUser(user: User): Result<Unit>
    fun getAllUsers(): Flow<List<User>>
    suspend fun getUserById(userId: String): User?
    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean): Result<Unit>
}