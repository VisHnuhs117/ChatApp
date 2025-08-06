package com.vishnuhs.chatapp.presentation.auth

import com.vishnuhs.chatapp.domain.model.User

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null
)