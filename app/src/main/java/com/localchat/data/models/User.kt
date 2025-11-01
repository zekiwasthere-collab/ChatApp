package com.localchat.data.models

import java.util.UUID

data class User(
    val userId: String = UUID.randomUUID().toString(),
    val username: String,
    val avatarColor: String
)
