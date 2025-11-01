package com.localchat.data.models

sealed class ChatEvent {
    data class UserJoined(val user: User, val timestamp: Long) : ChatEvent()
    data class UserLeft(val user: User, val timestamp: Long) : ChatEvent()
    data class TextMessage(val user: User, val message: String, val timestamp: Long) : ChatEvent()
    data class ImageMessage(val user: User, val imageData: String, val timestamp: Long) : ChatEvent()
    data class TypingIndicator(val user: User, val isTyping: Boolean) : ChatEvent()
    data class UserListUpdate(val users: List<User>) : ChatEvent()
    data class ConnectionStatusChanged(val status: ConnectionStatus) : ChatEvent()
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}
