package com.localchat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localchat.data.models.ConnectionStatus
import com.localchat.data.models.User
import com.localchat.data.repository.UserPreferencesRepository
import com.localchat.utils.NetworkUtils
import com.localchat.viewmodels.ChatViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun JoinScreen(
    username: String,
    avatarColor: String,
    chatViewModel: ChatViewModel,
    userPreferencesRepository: UserPreferencesRepository,
    onConnected: () -> Unit
) {
    var serverAddress by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(false) }

    val connectionStatus by chatViewModel.connectionStatus.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Load last server address
    LaunchedEffect(Unit) {
        val lastAddress = userPreferencesRepository.getLastServerAddress().first()
        if (lastAddress != null) {
            serverAddress = lastAddress
        } else {
            serverAddress = "192.168.43.1:8080" // Default hotspot address
        }
    }

    // Observe connection status
    LaunchedEffect(connectionStatus) {
        when (connectionStatus) {
            ConnectionStatus.CONNECTED -> {
                isConnecting = false
                onConnected()
            }
            ConnectionStatus.ERROR -> {
                isConnecting = false
                errorMessage = "Connection timeout. Check address and try again."
            }
            ConnectionStatus.CONNECTING -> {
                isConnecting = true
                errorMessage = null
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        Text(
            text = "Join Chat Room",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Instructions
        Text(
            text = "Connect to the host's WiFi hotspot first",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Server address input
        Text(
            text = "Server Address",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = serverAddress,
            onValueChange = {
                serverAddress = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("192.168.43.1:8080") },
            singleLine = true,
            enabled = !isConnecting,
            isError = errorMessage != null
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Connection status
        if (isConnecting) {
            Text(
                text = "Connecting...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Connect button
        Button(
            onClick = {
                // Validate server address
                if (!NetworkUtils.isValidIpAddress(serverAddress)) {
                    errorMessage = "Invalid address format. Use IP:PORT (e.g., 192.168.43.1:8080)"
                    return@Button
                }

                // Parse IP and port
                val parts = serverAddress.split(":")
                val ip = parts[0]
                val port = parts[1].toInt()

                // Create user and connect
                val user = User(
                    username = username,
                    avatarColor = avatarColor
                )

                coroutineScope.launch {
                    // Save address for next time
                    userPreferencesRepository.saveLastServerAddress(serverAddress)
                    // Connect
                    chatViewModel.connect(ip, port, user)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isConnecting && serverAddress.isNotEmpty()
        ) {
            Text(
                text = if (isConnecting) "Connecting..." else "Connect",
                fontSize = 18.sp
            )
        }
    }
}
