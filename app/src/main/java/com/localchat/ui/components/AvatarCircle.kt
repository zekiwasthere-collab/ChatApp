package com.localchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AvatarCircle(
    letter: String,
    color: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val avatarColor = try {
        Color(android.graphics.Color.parseColor(color))
    } catch (e: Exception) {
        Color.Gray
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.firstOrNull()?.uppercase() ?: "",
            color = Color.White,
            fontSize = (size.value / 2).sp,
            fontWeight = FontWeight.Bold
        )
    }
}
