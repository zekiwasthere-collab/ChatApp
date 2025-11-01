package com.localchat.network

import android.content.Context
import com.localchat.data.models.ServerStatus
import com.localchat.data.models.User
import com.localchat.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class ChatServer(private val context: Context) {

    companion object {
        private const val PORT = 8080
    }

    private var serverSocket: ServerSocket? = null
    private val connectedClients = mutableMapOf<String, ClientConnection>()
    private val connectedUsers = mutableMapOf<String, User>()

    private val _serverStatus = MutableStateFlow(ServerStatus.STOPPED)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus

    private val _connectedUserCount = MutableStateFlow(0)
    val connectedUserCount: StateFlow<Int> = _connectedUserCount

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var serverJob: Job? = null

    /**
     * Start the server on port 8080
     * Returns the local IP address or null if failed
     */
    suspend fun start(): String? = withContext(Dispatchers.IO) {
        try {
            _serverStatus.value = ServerStatus.STARTING

            // Try to create server socket
            serverSocket = ServerSocket(PORT)

            // Get local IP address
            val ipAddress = NetworkUtils.getLocalIpAddress(context)
            if (ipAddress == null) {
                _errorMessage.value = "Cannot get IP address. Enable WiFi hotspot first."
                _serverStatus.value = ServerStatus.ERROR
                return@withContext null
            }

            _serverStatus.value = ServerStatus.RUNNING
            _errorMessage.value = null

            // Start accepting connections in background
            serverJob = CoroutineScope(Dispatchers.IO).launch {
                acceptConnections()
            }

            return@withContext "$ipAddress:$PORT"
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = if (e.message?.contains("Address already in use") == true) {
                "Port already in use. Close other apps using network."
            } else {
                "Failed to start server: ${e.message}"
            }
            _serverStatus.value = ServerStatus.ERROR
            return@withContext null
        }
    }

    /**
     * Accept incoming client connections in loop
     */
    private suspend fun acceptConnections() {
        try {
            while (serverSocket?.isClosed == false) {
                try {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null) {
                        // Handle client in separate coroutine
                        CoroutineScope(Dispatchers.IO).launch {
                            handleClient(clientSocket)
                        }
                    }
                } catch (e: Exception) {
                    if (serverSocket?.isClosed == false) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handle individual client connection
     * - Read messages from client
     * - Broadcast to all clients
     * - Handle disconnection
     */
    private suspend fun handleClient(socket: Socket) {
        var clientId: String? = null
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null

        try {
            reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            writer = PrintWriter(socket.getOutputStream(), true)

            // Read messages from client
            while (socket.isConnected && !socket.isClosed) {
                val line = reader.readLine() ?: break

                // Parse message
                val chatEvent = MessageProtocol.deserializeMessage(line)

                when (chatEvent) {
                    is com.localchat.data.models.ChatEvent.UserJoined -> {
                        clientId = chatEvent.user.userId
                        // Add client to connected clients
                        connectedClients[clientId] = ClientConnection(socket, writer)
                        connectedUsers[clientId] = chatEvent.user
                        _connectedUserCount.value = connectedUsers.size

                        // Send user list to new client
                        val userListEvent = com.localchat.data.models.ChatEvent.UserListUpdate(
                            connectedUsers.values.toList()
                        )
                        val userListJson = MessageProtocol.serializeMessage(userListEvent)
                        writer.println(userListJson)

                        // Broadcast user joined to all clients
                        broadcastMessage(line)
                    }
                    is com.localchat.data.models.ChatEvent.UserLeft -> {
                        // Broadcast user left to all clients
                        broadcastMessage(line)
                    }
                    else -> {
                        // Broadcast all other messages (text, image, typing)
                        broadcastMessage(line)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Clean up disconnected client
            if (clientId != null) {
                connectedClients.remove(clientId)
                val user = connectedUsers.remove(clientId)
                _connectedUserCount.value = connectedUsers.size

                // Broadcast user left
                if (user != null) {
                    val userLeftEvent = com.localchat.data.models.ChatEvent.UserLeft(
                        user,
                        System.currentTimeMillis()
                    )
                    val userLeftJson = MessageProtocol.serializeMessage(userLeftEvent)
                    broadcastMessage(userLeftJson)
                }
            }

            try {
                reader?.close()
                writer?.close()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Broadcast message to all connected clients
     */
    fun broadcastMessage(message: String) {
        val disconnectedClients = mutableListOf<String>()

        for ((clientId, connection) in connectedClients) {
            try {
                connection.writer.println(message)
                if (connection.writer.checkError()) {
                    disconnectedClients.add(clientId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                disconnectedClients.add(clientId)
            }
        }

        // Remove disconnected clients
        for (clientId in disconnectedClients) {
            connectedClients.remove(clientId)
            connectedUsers.remove(clientId)
        }
        if (disconnectedClients.isNotEmpty()) {
            _connectedUserCount.value = connectedUsers.size
        }
    }

    /**
     * Stop the server and disconnect all clients
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        try {
            // Close all client connections
            for ((_, connection) in connectedClients) {
                try {
                    connection.socket.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            connectedClients.clear()
            connectedUsers.clear()
            _connectedUserCount.value = 0

            // Close server socket
            serverSocket?.close()
            serverSocket = null

            // Cancel server job
            serverJob?.cancel()
            serverJob = null

            _serverStatus.value = ServerStatus.STOPPED
            _errorMessage.value = null
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Error stopping server: ${e.message}"
        }
    }

    /**
     * Get server address as formatted string
     */
    fun getServerAddress(): String {
        val ipAddress = NetworkUtils.getLocalIpAddress(context) ?: "Unknown"
        return "$ipAddress:$PORT"
    }

    /**
     * Data class to hold client connection info
     */
    private data class ClientConnection(
        val socket: Socket,
        val writer: PrintWriter
    )
}
