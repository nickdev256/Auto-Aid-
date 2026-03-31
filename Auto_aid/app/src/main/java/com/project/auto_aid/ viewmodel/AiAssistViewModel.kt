package com.project.auto_aid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.auto_aid.data.local.TokenStore
import com.project.auto_aid.data.network.RetrofitClient
import com.project.auto_aid.data.network.dto.AiAnalyzeRequest
import com.project.auto_aid.data.network.dto.AiAnalyzeResponse
import com.project.auto_aid.data.network.dto.AiEscalateResponse
import com.project.auto_aid.data.network.dto.AiFlags
import com.project.auto_aid.data.network.dto.AiLocation
import com.project.auto_aid.data.network.dto.AiProviderPayload
import com.project.auto_aid.data.network.dto.AiVehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiAssistViewModel(
    private val tokenStore: TokenStore
) : ViewModel() {

    private val api = RetrofitClient.create(tokenStore)

    private val _uiState = MutableStateFlow(AiAssistUiState())
    val uiState: StateFlow<AiAssistUiState> = _uiState.asStateFlow()

    fun updateInputText(value: String) {
        _uiState.value = _uiState.value.copy(inputText = value)
    }

    fun setVehicleType(value: String) {
        _uiState.value = _uiState.value.copy(vehicleType = value)
    }

    fun setFuelType(value: String) {
        _uiState.value = _uiState.value.copy(fuelType = value)
    }

    fun setAddress(value: String) {
        _uiState.value = _uiState.value.copy(address = value)
    }

    fun setLocation(lat: Double?, lng: Double?, address: String = _uiState.value.address) {
        _uiState.value = _uiState.value.copy(
            lat = lat,
            lng = lng,
            address = address
        )
    }

    fun setInjury(value: Boolean) {
        _uiState.value = _uiState.value.copy(injury = value)
    }

    fun setFire(value: Boolean) {
        _uiState.value = _uiState.value.copy(fire = value)
    }

    fun setFuelLeak(value: Boolean) {
        _uiState.value = _uiState.value.copy(fuelLeak = value)
    }

    fun setUnsafeLocation(value: Boolean) {
        _uiState.value = _uiState.value.copy(unsafeLocation = value)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearEscalationResult() {
        _uiState.value = _uiState.value.copy(escalationResult = null)
    }

    fun chooseProviderType(providerType: String) {
        _uiState.value = _uiState.value.copy(selectedProviderType = providerType)
        addSystemMessage("Chosen provider: $providerType")
    }

    private fun addUserMessage(text: String) {
        if (text.isBlank()) return
        _uiState.value = _uiState.value.copy(
            chatMessages = _uiState.value.chatMessages + ChatMessage(
                text = text,
                isUser = true,
                type = MessageType.NORMAL
            )
        )
    }

    private fun addAiMessage(text: String) {
        if (text.isBlank()) return
        _uiState.value = _uiState.value.copy(
            chatMessages = _uiState.value.chatMessages + ChatMessage(
                text = text,
                isUser = false,
                type = MessageType.NORMAL
            )
        )
    }

    private fun addSystemMessage(text: String) {
        if (text.isBlank()) return
        _uiState.value = _uiState.value.copy(
            chatMessages = _uiState.value.chatMessages + ChatMessage(
                text = text,
                isUser = false,
                type = MessageType.SYSTEM
            )
        )
    }

    fun sendMessage() {
        val message = _uiState.value.inputText.trim()
        if (message.isBlank()) return

        addUserMessage(message)
        _uiState.value = _uiState.value.copy(inputText = "")

        analyzeConversation(userMessage = message)
    }

    fun triggerProviderSearch(chosenProviderType: String? = null) {
        val state = _uiState.value
        val analysis = state.analysisResult?.analysis ?: run {
            addAiMessage("I need to understand the issue first before I search for a provider.")
            return
        }

        val selected = chosenProviderType
            ?: state.selectedProviderType
            ?: state.primaryProviderType
            ?: state.analysisResult?.providerOptions?.primaryProvider
            ?: state.analysisResult?.providerBrief?.recommendedService
            ?: analysis.serviceType

        if (selected.isBlank()) {
            addAiMessage("I need a bit more information before I can find the right provider.")
            return
        }

        addAiMessage("Alright 👍 I’ll help you find available $selected providers.")

        _uiState.value = _uiState.value.copy(
            selectedProviderType = selected,
            escalationResult = AiEscalateResponse(
                success = true,
                message = "OPEN_PROVIDER_SELECTION",
                providerPayload = AiProviderPayload(
                    action = "PROVIDER_REQUIRED",
                    recommendedService = selected,
                    alternativeProviders = state.alternativeProviderTypes,
                    selectableProviders = state.selectableProviderTypes,
                    priority = when (analysis.risk.lowercase()) {
                        "high", "critical", "emergency" -> "high"
                        else -> "normal"
                    },
                    location = AiLocation(
                        lat = state.lat,
                        lng = state.lng,
                        address = state.address.ifBlank { null }
                    ),
                    vehicle = AiVehicle(
                        type = state.vehicleType.ifBlank { null },
                        fuelType = state.fuelType.ifBlank { null }
                    ),
                    aiSummary = state.analysisResult?.providerBrief?.summary
                        ?: analysis.reason
                        ?: analysis.userMessage,
                    message = "User asked to find a provider."
                )
            )
        )
    }

    private fun analyzeConversation(userMessage: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val state = _uiState.value

                val res = api.analyzeProblem(
                    AiAnalyzeRequest(
                        message = userMessage,
                        flags = AiFlags(
                            injury = state.injury,
                            fire = state.fire,
                            fuelLeak = state.fuelLeak,
                            unsafeLocation = state.unsafeLocation
                        ),
                        vehicle = AiVehicle(
                            type = state.vehicleType.ifBlank { null },
                            fuelType = state.fuelType.ifBlank { null }
                        ),
                        location = AiLocation(
                            lat = state.lat,
                            lng = state.lng,
                            address = state.address.ifBlank { null }
                        ),
                        stage = "chat",
                        previousAnalysis = state.analysisResult
                    )
                )

                if (!res.isSuccessful) {
                    throw Exception("AI request failed (${res.code()})")
                }

                val response = res.body() ?: throw Exception("Empty AI response")

                val providerOptions = response.providerOptions
                val primaryProvider = providerOptions?.primaryProvider
                    ?: response.providerBrief?.recommendedService
                    ?: response.analysis.serviceType

                val alternatives = providerOptions?.alternativeProviders.orEmpty()
                val selectable = providerOptions?.userSelectableProviders
                    ?.takeIf { it.isNotEmpty() }
                    ?: buildList {
                        if (!primaryProvider.isNullOrBlank()) add(primaryProvider)
                        addAll(alternatives.filter { it.isNotBlank() && it != primaryProvider })
                    }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysisResult = response,
                    escalationResult = null,
                    primaryProviderType = primaryProvider,
                    alternativeProviderTypes = alternatives,
                    selectableProviderTypes = selectable.distinct(),
                    selectedProviderType = primaryProvider,
                    selfSolveAvailable = response.selfSolve.allowed,
                    selfSolveSteps = response.selfSolve.steps
                )

                handleAiResponse(response)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Something went wrong"
                )
                addAiMessage("I couldn’t process that properly. Please try again with a little more detail.")
            }
        }
    }

    private fun handleAiResponse(response: AiAnalyzeResponse) {
        val analysis = response.analysis
        val selfSolve = response.selfSolve
        val providerBrief = response.providerBrief
        val providerOptions = response.providerOptions
        val possibleProblems = response.possibleProblems.orEmpty()
        val followUpQuestions = response.followUpQuestions.orEmpty()

        when {
            analysis.userMessage.isNotBlank() -> addAiMessage(analysis.userMessage)
            !providerBrief?.summary.isNullOrBlank() -> addAiMessage(providerBrief.summary!!)
            else -> addAiMessage("I’ve understood your message. Tell me a little more about what the vehicle is doing.")
        }

        if (possibleProblems.isNotEmpty()) {
            addSystemMessage("Possible problems:")
            possibleProblems.forEach { addSystemMessage("• $it") }
        }

        if (analysis.issue.isNotBlank()) {
            addSystemMessage("Problem detected: ${analysis.issue}")
        }

        if (analysis.risk.isNotBlank()) {
            addSystemMessage("Risk level: ${analysis.risk}")
        }

        if (analysis.reason.isNotBlank()) {
            addSystemMessage("Why: ${analysis.reason}")
        }

        if (analysis.confidence > 0.0) {
            addSystemMessage("Confidence: ${(analysis.confidence * 100).toInt()}%")
        }

        if (analysis.action == "SELF_SOLVE_FIRST" && selfSolve.allowed && selfSolve.steps.isNotEmpty()) {
            addSystemMessage("Try these steps first:")
            selfSolve.steps.forEachIndexed { index, step ->
                addSystemMessage("${index + 1}. $step")
            }

            if (followUpQuestions.isNotEmpty()) {
                addSystemMessage("Follow-up question:")
                addSystemMessage(followUpQuestions.first())
            } else {
                addSystemMessage("Did the steps work? Please reply yes or no.")
            }
            return
        }

        if (analysis.action == "ASK_IF_WORKED") {
            if (followUpQuestions.isNotEmpty()) {
                addSystemMessage("Follow-up question:")
                addSystemMessage(followUpQuestions.first())
            } else {
                addSystemMessage("Did the steps work? Please reply yes or no.")
            }
            return
        }

        if (analysis.action == "RESOLVED") {
            addSystemMessage("Good news: the issue seems resolved.")
            return
        }

        val primary = _uiState.value.primaryProviderType
        val alternatives = _uiState.value.alternativeProviderTypes

        if (!primary.isNullOrBlank()) {
            addSystemMessage("Best provider match: $primary")
        }

        if (alternatives.isNotEmpty()) {
            addSystemMessage("Other possible providers: ${alternatives.joinToString(", ")}")
        }

        if (!providerOptions?.reason.isNullOrBlank()) {
            addSystemMessage("Provider choice note: ${providerOptions.reason}")
        }

        if (
            analysis.action == "PROVIDER_REQUIRED" ||
            analysis.action == "EMERGENCY"
        ) {
            addSystemMessage("You can continue with the recommended provider or choose another suitable option.")
            addSystemMessage("Search for available service providers")
        }
    }

    fun resetChat() {
        _uiState.value = AiAssistUiState()
    }
}
