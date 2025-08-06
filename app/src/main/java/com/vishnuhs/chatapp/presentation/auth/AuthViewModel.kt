package com.vishnuhs.chatapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishnuhs.chatapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val isLoggedIn = currentUser != null
                println("DEBUG: checkAuthState - User: ${currentUser?.email}, isLoggedIn: $isLoggedIn")

                authRepository.isUserLoggedIn()
                    .collect { isLoggedInFromRepo ->
                        println("DEBUG: From repository - isLoggedIn: $isLoggedInFromRepo")
                        _isLoggedIn.value = isLoggedInFromRepo
                    }
            } catch (e: Exception) {
                println("DEBUG: checkAuthState error: ${e.message}")
                _error.value = e.message
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Please fill in all fields"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                authRepository.signIn(email, password)
                    .collect { resultFlow ->
                        resultFlow.collect { success ->
                            if (success) {
                                _isLoggedIn.value = true
                                _error.value = null
                            } else {
                                _error.value = "Invalid email or password"
                            }
                        }
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Sign in failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _error.value = "Please fill in all fields"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val success = authRepository.signUp(email, password, name)
                if (success) {
                    _isLoggedIn.value = true
                    _error.value = null
                } else {
                    _error.value = "Sign up failed"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Sign up failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                // Debug: Check current auth state
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                println("DEBUG: Before logout - User: ${currentUser?.email}")

                // Direct Firebase logout
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

                // Debug: Check auth state after logout
                val afterLogout = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                println("DEBUG: After logout - User: ${afterLogout?.email}")

                // Force update the state
                _isLoggedIn.value = false
                _error.value = null

                println("DEBUG: Set isLoggedIn to false")

                // Recheck auth state to make sure
                checkAuthState()
            } catch (e: Exception) {
                println("DEBUG: Logout error: ${e.message}")
                _error.value = e.message
                // Force logout even if there's an error
                _isLoggedIn.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}