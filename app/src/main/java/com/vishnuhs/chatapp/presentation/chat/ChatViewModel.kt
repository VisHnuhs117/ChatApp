package com.vishnuhs.chatapp.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vishnuhs.chatapp.domain.model.Message
import com.vishnuhs.chatapp.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentChatUserId: String = ""

    fun loadMessages(chatUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        currentChatUserId = chatUserId

        viewModelScope.launch {
            chatRepository.getMessages(currentUserId, chatUserId)
                .catch { exception ->
                    _error.value = exception.message
                }
                .collect { messageList ->
                    _messages.value = messageList
                }
        }
    }

    fun sendMessage(content: String, chatUserId: String, chatUserName: String) {
        val currentUser = auth.currentUser ?: return
        if (content.isBlank()) return

        val message = Message(
            id = "", // Firestore will generate this
            senderId = currentUser.uid,
            receiverId = chatUserId,
            content = content.trim(),
            timestamp = System.currentTimeMillis(),
            senderName = currentUser.displayName ?: "You",
            isRead = false
        )

        viewModelScope.launch {
            _isLoading.value = true

            // Optimistic update - add message to UI immediately
            val currentMessages = _messages.value.toMutableList()
            val tempMessage = message.copy(id = "temp_${System.currentTimeMillis()}")
            currentMessages.add(tempMessage)
            _messages.value = currentMessages

            // Send to Firebase
            chatRepository.sendMessage(message)
                .onSuccess {
                    // Message sent successfully
                    _error.value = null
                    // The Firebase listener will automatically update with the real message
                    // and replace the temporary one
                }
                .onFailure { exception ->
                    // Remove the optimistic message on failure
                    val updatedMessages = _messages.value.toMutableList()
                    updatedMessages.removeAll { it.id == tempMessage.id }
                    _messages.value = updatedMessages
                    _error.value = "Failed to send message: ${exception.message}"
                }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}