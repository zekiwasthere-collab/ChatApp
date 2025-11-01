package com.localchat.viewmodels

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localchat.data.models.ChatEvent
import com.localchat.data.models.ConnectionStatus
import com.localchat.data.models.Message
import com.localchat.data.models.MessageContent
import com.localchat.data.models.User
import com.localchat.network.ChatClient
import com.localchat.network.MessageProtocol
import com.localchat.utils.ImageCompressor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val context: Context) : ViewModel() {

    private val chatClient = ChatClient()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _connectedUsers = MutableStateFlow<List<User>>(emptyList())
    val connectedUsers: StateFlow<List<User>> = _connectedUsers

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _typingUsers = MutableStateFlow<Set<String>>(emptySet())
    val typingUsers: StateFlow<Set<String>> = _typingUsers

    private var typingJob: Job? = null
    private val typingTimeouts = mutableMapOf<String, Job>()

    init {
        // Observe chat events from client
        viewModelScope.launch {
            chatClient.chatEvents.collect { event ->
                handleChatEvent(event)
            }
        }

        // Observe connection status
        viewModelScope.launch {
            chatClient.connectionStatus.collect { status ->
                _connectionStatus.value = status
            }
        }
    }

    /**
     * Connect to chat server
     */
    fun connect(serverAddress: String, port: Int, user: User) {
        viewModelScope.launch {
            chatClient.connect(serverAddress, port, user)
        }
    }

    /**
     * Handle incoming chat events
     */
    private fun handleChatEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.UserJoined -> {
                // Add system message
                val systemMessage = Message(
                    user = event.user,
                    content = MessageContent.System("${event.user.username} joined"),
                    timestamp = event.timestamp
                )
                _messages.value = _messages.value + systemMessage
            }
            is ChatEvent.UserLeft -> {
                // Add system message
                val systemMessage = Message(
                    user = event.user,
                    content = MessageContent.System("${event.user.username} left"),
                    timestamp = event.timestamp
                )
                _messages.value = _messages.value + systemMessage

                // Remove from typing users if present
                _typingUsers.value = _typingUsers.value - event.user.username
            }
            is ChatEvent.TextMessage -> {
                // Add text message
                val message = Message(
                    user = event.user,
                    content = MessageContent.Text(event.message),
                    timestamp = event.timestamp
                )
                _messages.value = _messages.value + message

                // Remove from typing users
                _typingUsers.value = _typingUsers.value - event.user.username
            }
            is ChatEvent.ImageMessage -> {
                // Decode and add image message
                val bitmap = MessageProtocol.decompressImage(event.imageData)
                if (bitmap != null) {
                    val message = Message(
                        user = event.user,
                        content = MessageContent.Image(bitmap),
                        timestamp = event.timestamp
                    )
                    _messages.value = _messages.value + message
                } else {
                    // Failed to decode, add error message
                    val errorMessage = Message(
                        user = event.user,
                        content = MessageContent.System("Image unavailable"),
                        timestamp = event.timestamp
                    )
                    _messages.value = _messages.value + errorMessage
                }

                // Remove from typing users
                _typingUsers.value = _typingUsers.value - event.user.username
            }
            is ChatEvent.TypingIndicator -> {
                if (event.isTyping) {
                    // Add to typing users
                    _typingUsers.value = _typingUsers.value + event.user.username

                    // Remove after 5 seconds if no update
                    typingTimeouts[event.user.username]?.cancel()
                    typingTimeouts[event.user.username] = viewModelScope.launch {
                        delay(5000)
                        _typingUsers.value = _typingUsers.value - event.user.username
                        typingTimeouts.remove(event.user.username)
                    }
                } else {
                    // Remove from typing users
                    _typingUsers.value = _typingUsers.value - event.user.username
                    typingTimeouts[event.user.username]?.cancel()
                    typingTimeouts.remove(event.user.username)
                }
            }
            is ChatEvent.UserListUpdate -> {
                // Update connected users
                _connectedUsers.value = event.users
            }
            is ChatEvent.ConnectionStatusChanged -> {
                // Status is already handled by direct observation
            }
        }
    }

    /**
     * Send text message
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            chatClient.sendTextMessage(text)
            // Stop typing indicator
            onStopTyping()
        }
    }

    /**
     * Send image from URI
     */
    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            try {
                // Load and compress image
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    // Send image
                    chatClient.sendImageMessage(bitmap)
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * User is typing
     * Debounced to send indicator max every 2 seconds
     */
    fun onTyping() {
        if (typingJob?.isActive == true) {
            return
        }

        typingJob = viewModelScope.launch {
            chatClient.sendTypingIndicator(true)
            delay(2000) // Debounce for 2 seconds
        }
    }

    /**
     * User stopped typing
     */
    fun onStopTyping() {
        typingJob?.cancel()
        typingJob = null

        viewModelScope.launch {
            chatClient.sendTypingIndicator(false)
        }
    }

    /**
     * Disconnect from chat
     */
    fun disconnect() {
        viewModelScope.launch {
            chatClient.disconnect()
        }
    }

    /**
     * Clean up when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            chatClient.disconnect()
        }
    }
}
