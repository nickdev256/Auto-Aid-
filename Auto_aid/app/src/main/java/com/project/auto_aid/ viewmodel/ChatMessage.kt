package com.project.auto_aid.viewmodel

import java.util.UUID

enum class MessageType {
    NORMAL,
    SYSTEM
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val type: MessageType = MessageType.NORMAL,
    val id: String = UUID.randomUUID().toString()
)