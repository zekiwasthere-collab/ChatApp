package com.localchat.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.localchat.data.models.ConnectionStatus
import com.localchat.data.models.User
import com.localchat.ui.components.MessageItem
import com.localchat.viewmodels.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    mode: String,
    username: String,
    avatarColor: String,
    serverAddress: String,
    chatViewModel: ChatViewModel,
    onExit: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var showExitDialog by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var fullScreenImage by remember { mutableStateOf<Bitmap?>(null) }

    val messages by chatViewModel.messages.collectAsState()
    val connectedUsers by chatViewModel.connectedUsers.collectAsState()
    val connectionStatus by chatViewModel.connectionStatus.collectAsState()
    val typingUsers by chatViewModel.typingUsers.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Permission launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted
        }
    }

    // Camera launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            chatViewModel.sendImage(Uri.EMPTY) // Will be handled differently
        }
    }

    // Gallery launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // Connect on launch
    LaunchedEffect(Unit) {
        val user = User(
            username = username,
            avatarColor = avatarColor
        )

        // Determine server address based on mode
        val address = if (mode == "host") {
            "127.0.0.1" // Localhost for host
        } else {
            serverAddress.split(":")[0] // Parse IP from address
        }

        val port = if (mode == "host") {
            8080
        } else {
            serverAddress.split(":").getOrNull(1)?.toIntOrNull() ?: 8080
        }

        chatViewModel.connect(address, port, user)
    }

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Send image when selected
    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            chatViewModel.sendImage(uri)
            selectedImageUri = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Room") },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Connection status indicator
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when (connectionStatus) {
                                    ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
                                    ConnectionStatus.RECONNECTING -> Color(0xFFFFA000)
                                    else -> Color.Red
                                }
                            )
                    )

                    // User count badge
                    BadgedBox(
                        badge = {
                            if (connectedUsers.isNotEmpty()) {
                                Badge {
                                    Text("${connectedUsers.size}")
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = "users",
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState
            ) {
                items(messages) { message ->
                    MessageItem(
                        message = message,
                        onImageClick = { bitmap ->
                            fullScreenImage = bitmap
                        }
                    )
                }

                // Typing indicator
                if (typingUsers.isNotEmpty()) {
                    item {
                        Text(
                            text = "${typingUsers.first()} is typing...",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach button
                IconButton(onClick = { showImagePicker = true }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                }

                // Text input
                OutlinedTextField(
                    value = messageText,
                    onValueChange = {
                        messageText = it
                        if (it.isNotEmpty()) {
                            chatViewModel.onTyping()
                        } else {
                            chatViewModel.onStopTyping()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            chatViewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray
                    )
                }
            }
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave chat room?") },
            text = { Text("Are you sure you want to leave the chat?") },
            confirmButton = {
                TextButton(onClick = {
                    chatViewModel.disconnect()
                    onExit()
                }) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Image picker bottom sheet
    if (showImagePicker) {
        ModalBottomSheet(
            onDismissRequest = { showImagePicker = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Image Source",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Camera option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showImagePicker = false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            takePictureLauncher.launch(null)
                        }
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "📷 Camera",
                        fontSize = 16.sp
                    )
                }

                // Gallery option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showImagePicker = false
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_IMAGES
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            storagePermissionLauncher.launch(permission)
                            pickImageLauncher.launch("image/*")
                        }
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "🖼️ Gallery",
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }

    // Full screen image dialog
    if (fullScreenImage != null) {
        Dialog(onDismissRequest = { fullScreenImage = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { fullScreenImage = null },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = fullScreenImage!!.asImageBitmap(),
                    contentDescription = "Full screen image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
