package com.project.auto_aid.data.network.dto

data class AiFlags(
    val injury: Boolean = false,
    val fire: Boolean = false,
    val fuelLeak: Boolean = false,
    val unsafeLocation: Boolean = false
)

data class AiVehicle(
    val type: String? = null,
    val fuelType: String? = null
)

data class AiLocation(
    val lat: Double? = null,
    val lng: Double? = null,
    val address: String? = null
)

data class AiAnalyzeRequest(
    val message: String,
    val flags: AiFlags? = null,
    val vehicle: AiVehicle? = null,
    val location: AiLocation? = null,
    val stage: String? = "chat",
    val previousAnalysis: AiAnalyzeResponse? = null
)


data class AiAnalyzeResponse(
    val success: Boolean,
    val analysis: AiAnalysis,
    val selfSolve: AiSelfSolve,
    val inputSummary: AiInputSummary? = null,
    val providerBrief: AiProviderBrief? = null,
    val followUpQuestions: List<String>? = null,
    val possibleProblems: List<String>? = null,
    val providerOptions: AiProviderOptions? = null
)

data class AiInputSummary(
    val message: String,
    val vehicle: AiVehicle? = null,
    val hasLocation: Boolean = false,
    val flags: AiFlags? = null
)

data class AiAnalysis(
    val issue: String = "",
    val symptoms: List<String> = emptyList(),
    val risk: String = "",
    val serviceType: String = "",
    val action: String = "",
    val confidence: Double = 0.0,
    val reason: String = "",
    val userMessage: String = ""
)

data class AiSelfSolve(
    val allowed: Boolean = false,
    val steps: List<String> = emptyList()
)

data class AiProviderBrief(
    val issue: String? = null,
    val symptoms: List<String>? = null,
    val risk: String? = null,
    val recommendedService: String? = null,
    val summary: String? = null
)

data class AiProviderOptions(
    val primaryProvider: String? = null,
    val alternativeProviders: List<String> = emptyList(),
    val userSelectableProviders: List<String> = emptyList(),
    val providerChoiceRequired: Boolean = false,
    val reason: String? = null
)

data class AiEscalateRequest(
    val originalAnalysis: AiAnalyzeResponse,
    val chosenProviderType: String? = null,
    val message: String? = null,
    val location: AiLocation? = null,
    val vehicle: AiVehicle? = null
)

data class AiEscalateResponse(
    val success: Boolean,
    val message: String,
    val providerPayload: AiProviderPayload
)

data class AiProviderPayload(
    val action: String,
    val recommendedService: String,
    val alternativeProviders: List<String> = emptyList(),
    val selectableProviders: List<String> = emptyList(),
    val priority: String,
    val location: AiLocation? = null,
    val vehicle: AiVehicle? = null,
    val aiSummary: String,
    val message: String
)