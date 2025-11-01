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
import com.localchat.data.repository.UserPreferencesRepository
import com.localchat.ui.components.ColorPicker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun UserSetupScreen(
    mode: String,
    onContinue: (username: String, avatarColor: String) -> Unit,
    userPreferencesRepository: UserPreferencesRepository
) {
    var username by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#2196F3") } // Default blue
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Load saved preferences
    LaunchedEffect(Unit) {
        val savedUsername = userPreferencesRepository.getUsername().first()
        val savedColor = userPreferencesRepository.getAvatarColor().first()

        if (savedUsername != null) {
            username = savedUsername
        }
        if (savedColor != null) {
            selectedColor = savedColor
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
            text = "Setup Profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Username input
        Text(
            text = "Your Name",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your name") },
            singleLine = true,
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

        Spacer(modifier = Modifier.height(32.dp))

        // Avatar color selector
        Text(
            text = "Choose Avatar Color",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        ColorPicker(
            selectedColor = selectedColor,
            onColorSelected = { selectedColor = it }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Continue button
        Button(
            onClick = {
                // Validate username
                val trimmedUsername = username.trim()
                when {
                    trimmedUsername.isEmpty() -> {
                        errorMessage = "Username cannot be empty"
                    }
                    trimmedUsername.length > 20 -> {
                        errorMessage = "Username must be less than 20 characters"
                    }
                    else -> {
                        // Save preferences and continue
                        coroutineScope.launch {
                            userPreferencesRepository.saveUsername(trimmedUsername)
                            userPreferencesRepository.saveAvatarColor(selectedColor)
                            onContinue(trimmedUsername, selectedColor)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Continue",
                fontSize = 18.sp
            )
        }
    }
}
