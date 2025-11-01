package com.localchat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localchat.data.models.ServerStatus
import com.localchat.viewmodels.ServerViewModel

@Composable
fun HostSetupScreen(
    username: String,
    avatarColor: String,
    serverViewModel: ServerViewModel,
    onEnterChat: () -> Unit
) {
    val serverStatus by serverViewModel.serverStatus.collectAsState()
    val serverAddress by serverViewModel.serverAddress.collectAsState()
    val connectedUserCount by serverViewModel.connectedUserCount.collectAsState()
    val errorMessage by serverViewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        Text(
            text = "Host Chat Room",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Instructions
        Text(
            text = "Enable WiFi hotspot on your phone, then start the server",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Server address card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Server Address",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (serverAddress.isNotEmpty()) serverAddress else "Not started",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Server status
        val statusText = when (serverStatus) {
            ServerStatus.STOPPED -> "Server Status: Stopped"
            ServerStatus.STARTING -> "Server Status: Starting..."
            ServerStatus.RUNNING -> "Server Status: Running ✓"
            ServerStatus.ERROR -> "Server Status: Error"
        }

        val statusColor = when (serverStatus) {
            ServerStatus.STOPPED -> Color.Gray
            ServerStatus.STARTING -> Color(0xFFFFA000)
            ServerStatus.RUNNING -> Color(0xFF4CAF50)
            ServerStatus.ERROR -> Color.Red
        }

        Text(
            text = statusText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = statusColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (serverStatus == ServerStatus.RUNNING) {
            Text(
                text = "Connected users: $connectedUserCount",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // Error message
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = Color.Red,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Start/Stop server button
        if (serverStatus == ServerStatus.STOPPED || serverStatus == ServerStatus.ERROR) {
            Button(
                onClick = { serverViewModel.startServer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = serverStatus != ServerStatus.STARTING
            ) {
                Text(
                    text = "Start Server",
                    fontSize = 18.sp
                )
            }
        } else if (serverStatus == ServerStatus.RUNNING) {
            OutlinedButton(
                onClick = { serverViewModel.stopServer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Stop Server",
                    fontSize = 18.sp
                )
            }
        }

        // Enter chat room button (only when server running)
        if (serverStatus == ServerStatus.RUNNING) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onEnterChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Enter Chat Room",
                    fontSize = 18.sp
                )
            }
        }
    }
}
