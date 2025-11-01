package com.localchat.network

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.localchat.data.models.ChatEvent
import com.localchat.data.models.User
import java.io.ByteArrayOutputStream

object MessageProtocol {

    private val gson = Gson()

    /**
     * Serialize ChatEvent to JSON string
     * Converts ChatEvent to JSON according to protocol specification
     */
    fun serializeMessage(chatEvent: ChatEvent): String {
        val json = JsonObject()

        when (chatEvent) {
            is ChatEvent.UserJoined -> {
                json.addProperty("type", "user_join")
                json.addProperty("userId", chatEvent.user.userId)
                json.addProperty("username", chatEvent.user.username)
                json.addProperty("avatarColor", chatEvent.user.avatarColor)
                json.addProperty("timestamp", chatEvent.timestamp)
            }
            is ChatEvent.UserLeft -> {
                json.addProperty("type", "user_leave")
                json.addProperty("userId", chatEvent.user.userId)
                json.addProperty("username", chatEvent.user.username)
                json.addProperty("timestamp", chatEvent.timestamp)
            }
            is ChatEvent.TextMessage -> {
                json.addProperty("type", "text_message")
                json.addProperty("userId", chatEvent.user.userId)
                json.addProperty("username", chatEvent.user.username)
                json.addProperty("avatarColor", chatEvent.user.avatarColor)
                json.addProperty("message", chatEvent.message)
                json.addProperty("timestamp", chatEvent.timestamp)
            }
            is ChatEvent.ImageMessage -> {
                json.addProperty("type", "image_message")
                json.addProperty("userId", chatEvent.user.userId)
                json.addProperty("username", chatEvent.user.username)
                json.addProperty("avatarColor", chatEvent.user.avatarColor)
                json.addProperty("imageData", chatEvent.imageData)
                json.addProperty("timestamp", chatEvent.timestamp)
            }
            is ChatEvent.TypingIndicator -> {
                json.addProperty("type", "typing")
                json.addProperty("userId", chatEvent.user.userId)
                json.addProperty("username", chatEvent.user.username)
                json.addProperty("isTyping", chatEvent.isTyping)
            }
            is ChatEvent.UserListUpdate -> {
                json.addProperty("type", "user_list")
                val usersArray = gson.toJsonTree(chatEvent.users)
                json.add("users", usersArray)
            }
            else -> {
                // ConnectionStatusChanged is not sent over network
            }
        }

        return gson.toJson(json)
    }

    /**
     * Deserialize JSON string to ChatEvent
     * Parse JSON string and convert to appropriate ChatEvent type
     */
    fun deserializeMessage(jsonString: String): ChatEvent? {
        try {
            val json = JsonParser.parseString(jsonString).asJsonObject
            val type = json.get("type")?.asString ?: return null

            return when (type) {
                "user_join" -> {
                    val user = User(
                        userId = json.get("userId").asString,
                        username = json.get("username").asString,
                        avatarColor = json.get("avatarColor").asString
                    )
                    val timestamp = json.get("timestamp").asLong
                    ChatEvent.UserJoined(user, timestamp)
                }
                "user_leave" -> {
                    val user = User(
                        userId = json.get("userId").asString,
                        username = json.get("username").asString,
                        avatarColor = "#000000" // Default color for leave events
                    )
                    val timestamp = json.get("timestamp").asLong
                    ChatEvent.UserLeft(user, timestamp)
                }
                "text_message" -> {
                    val user = User(
                        userId = json.get("userId").asString,
                        username = json.get("username").asString,
                        avatarColor = json.get("avatarColor").asString
                    )
                    val message = json.get("message").asString
                    val timestamp = json.get("timestamp").asLong
                    ChatEvent.TextMessage(user, message, timestamp)
                }
                "image_message" -> {
                    val user = User(
                        userId = json.get("userId").asString,
                        username = json.get("username").asString,
                        avatarColor = json.get("avatarColor").asString
                    )
                    val imageData = json.get("imageData").asString
                    val timestamp = json.get("timestamp").asLong
                    ChatEvent.ImageMessage(user, imageData, timestamp)
                }
                "typing" -> {
                    val user = User(
                        userId = json.get("userId").asString,
                        username = json.get("username").asString,
                        avatarColor = "#000000" // Default color for typing events
                    )
                    val isTyping = json.get("isTyping").asBoolean
                    ChatEvent.TypingIndicator(user, isTyping)
                }
                "user_list" -> {
                    val usersArray = json.getAsJsonArray("users")
                    val users = mutableListOf<User>()
                    for (userElement in usersArray) {
                        val userObj = userElement.asJsonObject
                        users.add(
                            User(
                                userId = userObj.get("userId").asString,
                                username = userObj.get("username").asString,
                                avatarColor = userObj.get("avatarColor").asString
                            )
                        )
                    }
                    ChatEvent.UserListUpdate(users)
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Compress image to JPEG and encode to base64
     * - Compress bitmap to JPEG (80% quality, max 800px width)
     * - Encode to base64 string
     */
    fun compressImage(bitmap: Bitmap): String {
        // Scale down if needed
        val maxWidth = 800
        val scaledBitmap = if (bitmap.width > maxWidth) {
            val scaleFactor = maxWidth.toFloat() / bitmap.width
            val newHeight = (bitmap.height * scaleFactor).toInt()
            Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
        } else {
            bitmap
        }

        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()

        // Clean up if we created a scaled version
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        // Encode to base64
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Decode base64 string to Bitmap
     * - Decode base64 to byte array
     * - Create Bitmap from byte array
     */
    fun decompressImage(base64: String): Bitmap? {
        return try {
            val byteArray = Base64.decode(base64, Base64.NO_WRAP)
            android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
