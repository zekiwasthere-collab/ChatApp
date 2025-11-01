package com.localchat.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localchat.data.models.ServerStatus
import com.localchat.network.ChatServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServerViewModel(context: Context) : ViewModel() {

    private val chatServer = ChatServer(context)

    private val _serverStatus = MutableStateFlow(ServerStatus.STOPPED)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus

    private val _serverAddress = MutableStateFlow("")
    val serverAddress: StateFlow<String> = _serverAddress

    private val _connectedUserCount = MutableStateFlow(0)
    val connectedUserCount: StateFlow<Int> = _connectedUserCount

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        // Observe server state flows
        viewModelScope.launch {
            chatServer.serverStatus.collect { status ->
                _serverStatus.value = status
            }
        }

        viewModelScope.launch {
            chatServer.connectedUserCount.collect { count ->
                _connectedUserCount.value = count
            }
        }

        viewModelScope.launch {
            chatServer.errorMessage.collect { error ->
                _errorMessage.value = error
            }
        }
    }

    /**
     * Start the chat server
     */
    fun startServer() {
        viewModelScope.launch {
            val address = chatServer.start()
            if (address != null) {
                _serverAddress.value = address
            }
        }
    }

    /**
     * Stop the chat server
     */
    fun stopServer() {
        viewModelScope.launch {
            chatServer.stop()
            _serverAddress.value = ""
        }
    }

    /**
     * Get formatted server address
     */
    fun getServerAddress(): String {
        return chatServer.getServerAddress()
    }

    /**
     * Clean up when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            chatServer.stop()
        }
    }
}
