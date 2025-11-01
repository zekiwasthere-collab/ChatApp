package com.localchat.data.models

import android.graphics.Bitmap
import java.util.UUID

data class Message(
    val messageId: String = UUID.randomUUID().toString(),
    val user: User,
    val content: MessageContent,
    val timestamp: Long
)

sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Image(val bitmap: Bitmap) : MessageContent()
    data class System(val text: String) : MessageContent()
}
