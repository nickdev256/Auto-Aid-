data class AiAnalysis(
    val issue: String,
    val risk: String,
    val action: String,
    val serviceType: String,
    val confidence: Double,
    val userMessage: String,
    val reason: String
)