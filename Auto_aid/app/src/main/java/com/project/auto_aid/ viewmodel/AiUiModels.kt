package com.project.auto_aid.viewmodel

data class AiAnalysis(
    val issue: String = "",
    val risk: String = "",
    val reason: String = "",
    val action: String = "",
    val confidence: Double = 0.0
)

data class AiAnalysisResult(
    val analysis: AiAnalysis = AiAnalysis()
)