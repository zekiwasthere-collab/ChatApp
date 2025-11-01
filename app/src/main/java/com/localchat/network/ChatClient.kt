package com.localchat.network

import android.graphics.Bitmap
import com.localchat.data.models.ChatEvent
import com.localchat.data.models.ConnectionStatus
import com.localchat.data.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class ChatClient {

    companion object {
        private const val CONNECTION_TIMEOUT = 10000L // 10 seconds
        private const val RECONNECTION_DELAY = 3000L // 3 seconds
        private const val MAX_RECONNECTION_ATTEMPTS = 3
    }

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    private var currentUser: User? = null
    private var serverAddress: String = ""
    private var serverPort: Int = 8080

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _chatEvents = MutableSharedFlow<ChatEvent>()
    val chatEvents: SharedFlow<ChatEvent> = _chatEvents

    private var receiveJob: Job? = null
    private var reconnectionAttempts = 0

    /**
     * Connect to server
     * @param serverAddress Server IP address
     * @param port Server port
     * @param user Current user info
     */
    suspend fun connect(serverAddress: String, port: Int, user: User) {
        this.serverAddress = serverAddress
        this.serverPort = port
        this.currentUser = user

        performConnection()
    }

    /**
     * Perform the actual connection
     */
    private suspend fun performConnection() = withContext(Dispatchers.IO) {
        try {
            _connectionStatus.value = ConnectionStatus.CONNECTING

            // Create socket with timeout
            socket = withTimeoutOrNull(CONNECTION_TIMEOUT) {
                val newSocket = Socket()
                newSocket.connect(InetSocketAddress(serverAddress, serverPort), CONNECTION_TIMEOUT.toInt())
                newSocket
            }

            if (socket == null) {
                _connectionStatus.value = ConnectionStatus.ERROR
                _chatEvents.emit(ChatEvent.ConnectionStatusChanged(ConnectionStatus.ERROR))
                return@withContext
            }

            // Set up reader and writer
            reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
            writer = PrintWriter(socket?.getOutputStream(), true)

            _connectionStatus.value = ConnectionStatus.CONNECTED
            _chatEvents.emit(ChatEvent.ConnectionStatusChanged(ConnectionStatus.CONNECTED))

            // Reset reconnection attempts on successful connection
            reconnectionAttempts = 0

            // Send user join message
            currentUser?.let { user ->
                val joinEvent = ChatEvent.UserJoined(user, System.currentTimeMillis())
                val jsonMessage = MessageProtocol.serializeMessage(joinEvent)
                writer?.println(jsonMessage)
            }

            // Start receiving messages
            receiveJob = CoroutineScope(Dispatchers.IO).launch {
                receiveMessages()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            _connectionStatus.value = ConnectionStatus.ERROR
            _chatEvents.emit(ChatEvent.ConnectionStatusChanged(ConnectionStatus.ERROR))
        }
    }

    /**
     * Receive messages from server continuously
     */
    private suspend fun receiveMessages() {
        try {
            while (socket?.isConnected == true && !socket!!.isClosed) {
                val line = reader?.readLine()
                if (line == null) {
                    // Connection lost
                    handleConnectionLost()
                    break
                }

                // Parse and emit chat event
                val chatEvent = MessageProtocol.deserializeMessage(line)
                if (chatEvent != null) {
                    _chatEvents.emit(chatEvent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            handleConnectionLost()
        }
    }

    /**
     * Handle connection lost - attempt reconnection
     */
    private suspend fun handleConnectionLost() {
        if (_connectionStatus.value == ConnectionStatus.CONNECTED ||
            _connectionStatus.value == ConnectionStatus.RECONNECTING) {

            _connectionStatus.value = ConnectionStatus.RECONNECTING
            _chatEvents.emit(ChatEvent.ConnectionStatusChanged(ConnectionStatus.RECONNECTING))

            // Clean up current connection
            cleanupConnection()

            // Attempt reconnection
            while (reconnectionAttempts < MAX_RECONNECTION_ATTEMPTS) {
                reconnectionAttempts++
                delay(RECONNECTION_DELAY)

                try {
                    performConnection()
                    if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                        return // Reconnection successful
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Max reconnection attempts reached
            _connectionStatus.value = ConnectionStatus.ERROR
            _chatEvents.emit(ChatEvent.ConnectionStatusChanged(ConnectionStatus.ERROR))
        }
    }

    /**
     * Clean up connection resources
     */
    private fun cleanupConnection() {
        try {
            reader?.close()
            writer?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        reader = null
        writer = null
        socket = null
    }

    /**
     * Send text message
     */
    suspend fun sendTextMessage(message: String) {
        withContext(Dispatchers.IO) {
            try {
                currentUser?.let { user ->
                    val textEvent = ChatEvent.TextMessage(user, message, System.currentTimeMillis())
                    val jsonMessage = MessageProtocol.serializeMessage(textEvent)
                    writer?.println(jsonMessage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Send image message
     * @param bitmap Image bitmap to send
     */
    suspend fun sendImageMessage(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            try {
                currentUser?.let { user ->
                    // Compress and encode image to base64
                    val imageData = MessageProtocol.compressImage(bitmap)
                    val imageEvent = ChatEvent.ImageMessage(user, imageData, System.currentTimeMillis())
                    val jsonMessage = MessageProtocol.serializeMessage(imageEvent)
                    writer?.println(jsonMessage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Send typing indicator
     */
    suspend fun sendTypingIndicator(isTyping: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                currentUser?.let { user ->
                    val typingEvent = ChatEvent.TypingIndicator(user, isTyping)
                    val jsonMessage = MessageProtocol.serializeMessage(typingEvent)
                    writer?.println(jsonMessage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Disconnect from server gracefully
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            // Send user leave message
            currentUser?.let { user ->
                val leaveEvent = ChatEvent.UserLeft(user, System.currentTimeMillis())
                val jsonMessage = MessageProtocol.serializeMessage(leaveEvent)
                writer?.println(jsonMessage)
            }

            // Cancel receive job
            receiveJob?.cancel()
            receiveJob = null

            // Clean up connection
            cleanupConnection()

            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return _connectionStatus.value == ConnectionStatus.CONNECTED
    }
}
