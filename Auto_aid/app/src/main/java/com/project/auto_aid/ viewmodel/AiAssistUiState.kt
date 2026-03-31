package com.project.auto_aid.viewmodel

import com.project.auto_aid.data.network.dto.AiAnalyzeResponse
import com.project.auto_aid.data.network.dto.AiEscalateResponse

data class AiAssistUiState(
    val inputText: String = "",
    val vehicleType: String = "",
    val fuelType: String = "",
    val address: String = "",
    val lat: Double? = null,
    val lng: Double? = null,

    val injury: Boolean = false,
    val fire: Boolean = false,
    val fuelLeak: Boolean = false,
    val unsafeLocation: Boolean = false,

    val isLoading: Boolean = false,
    val isEscalating: Boolean = false,

    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "Hello 👋 Tell me what is happening with the vehicle, and I’ll help you step by step.",
            isUser = false,
            type = MessageType.NORMAL
        )
    ),

    val analysisResult: AiAnalyzeResponse? = null,
    val escalationResult: AiEscalateResponse? = null,

    val selectedProviderType: String? = null,
    val primaryProviderType: String? = null,
    val alternativeProviderTypes: List<String> = emptyList(),
    val selectableProviderTypes: List<String> = emptyList(),
    val selfSolveAvailable: Boolean = false,
    val selfSolveSteps: List<String> = emptyList(),

    val errorMessage: String? = null
)
