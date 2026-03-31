package com.project.auto_aid.ai

data class ProblemRule(
    val keywords: List<String>,
    val service: String,
    val severity: String, // minor, major, critical
    val canSelfSolve: Boolean,
    val emergency: Boolean = false,
    val secondaryServices: List<String> = emptyList(),
    val selfSteps: List<String> = emptyList(),
    val issueLabel: String? = null
)

data class ProviderRecommendation(
    val primary: String,
    val secondary: List<String> = emptyList()
)

data class AutoAidDecision(
    val issue: String,
    val service: String,
    val severity: String,
    val canSelfSolve: Boolean,
    val needsProvider: Boolean,
    val emergency: Boolean,
    val selfSteps: List<String> = emptyList(),
    val providerReason: String = "",
    val userMessage: String = "",
    val recommendation: ProviderRecommendation = ProviderRecommendation(primary = "garage")
)

object AiProblemClassifier {

    private val rules = listOf(

        // ---------------- GARAGE - MINOR / SELF HELP ----------------
        ProblemRule(
            keywords = listOf("flat tyre", "flat tire", "puncture", "tyre puncture", "tire puncture"),
            service = "garage",
            severity = "minor",
            canSelfSolve = true,
            issueLabel = "Flat tyre / puncture",
            selfSteps = listOf(
                "Park the vehicle in a safe place away from traffic.",
                "Turn on hazard lights.",
                "Inspect the damaged tyre carefully.",
                "Use the spare tyre if you have one and it is safe.",
                "If you have no spare or it is unsafe, request a provider."
            )
        ),
        ProblemRule(
            keywords = listOf("dead battery", "battery dead", "weak battery", "battery low"),
            service = "garage",
            severity = "minor",
            canSelfSolve = true,
            issueLabel = "Battery problem",
            selfSteps = listOf(
                "Check whether dashboard lights come on.",
                "Check battery terminals for looseness.",
                "Try a jump start if available and safe.",
                "If the car still does not start, request a provider."
            )
        ),
        ProblemRule(
            keywords = listOf("car won't start", "car wont start", "vehicle won't start", "vehicle wont start", "starter delay"),
            service = "garage",
            severity = "minor",
            canSelfSolve = true,
            secondaryServices = listOf("towing"),
            issueLabel = "Starting problem",
            selfSteps = listOf(
                "Ensure the car is in park or neutral.",
                "Check if the battery seems weak.",
                "Try restarting once or twice only.",
                "If it still fails, request a provider."
            )
        ),
        ProblemRule(
            keywords = listOf("overheating", "engine hot", "temperature high", "car overheating"),
            service = "garage",
            severity = "minor",
            canSelfSolve = true,
            secondaryServices = listOf("towing"),
            issueLabel = "Engine overheating",
            selfSteps = listOf(
                "Stop the vehicle in a safe place.",
                "Turn off the engine.",
                "Do not open the radiator cap immediately.",
                "Let the engine cool down.",
                "If smoke appears or overheating continues, request a provider."
            )
        ),
        ProblemRule(
            keywords = listOf("brake noise", "squeaky brakes", "brakes making noise"),
            service = "garage",
            severity = "minor",
            canSelfSolve = true,
            issueLabel = "Brake noise",
            selfSteps = listOf(
                "Reduce speed and avoid harsh braking.",
                "Check whether braking still responds normally.",
                "If braking feels weak or unsafe, stop and request a provider."
            )
        ),
        ProblemRule(
            keywords = listOf("oil warning", "low oil", "engine oil low", "oil light"),
            service = "garage",
            severity = "minor",
            canSelfSolve = true,
            issueLabel = "Low engine oil",
            selfSteps = listOf(
                "Stop in a safe place.",
                "Check the oil level if you know how.",
                "Add correct oil if available.",
                "If warning remains or there is visible leaking, request a provider."
            )
        ),
        ProblemRule(
            keywords = listOf("stalling", "car stalling", "engine stalling"),
            service = "garage",
            severity = "minor",
            canSelfSolve = true,
            secondaryServices = listOf("towing"),
            issueLabel = "Stalling issue",
            selfSteps = listOf(
                "Move to a safe area if possible.",
                "Try restarting once.",
                "Avoid long driving if the problem repeats.",
                "Request a provider if stalling continues."
            )
        ),

        // ---------------- GARAGE - MAJOR / PROVIDER ----------------
        ProblemRule(
            keywords = listOf("brake failure", "brakes failed", "no brakes"),
            service = "garage",
            severity = "major",
            canSelfSolve = false,
            secondaryServices = listOf("towing"),
            issueLabel = "Brake failure"
        ),
        ProblemRule(
            keywords = listOf("steering failure", "steering locked", "cannot steer", "cant steer"),
            service = "garage",
            severity = "major",
            canSelfSolve = false,
            secondaryServices = listOf("towing"),
            issueLabel = "Steering failure"
        ),
        ProblemRule(
            keywords = listOf("gearbox failure", "transmission failure", "gear not shifting", "gear stuck"),
            service = "garage",
            severity = "major",
            canSelfSolve = false,
            secondaryServices = listOf("towing"),
            issueLabel = "Transmission problem"
        ),
        ProblemRule(
            keywords = listOf("engine failure", "engine knocked", "engine smoke", "heavy oil leak", "alternator failure"),
            service = "garage",
            severity = "major",
            canSelfSolve = false,
            secondaryServices = listOf("towing"),
            issueLabel = "Serious engine problem"
        ),

        // ---------------- FUEL - MINOR / SELF HELP ----------------
        ProblemRule(
            keywords = listOf("low fuel", "fuel almost finished", "forgot to refuel", "almost no fuel"),
            service = "fuel",
            severity = "minor",
            canSelfSolve = true,
            issueLabel = "Low fuel",
            selfSteps = listOf(
                "Check your fuel level.",
                "Reduce unnecessary movement.",
                "If a station is near and safe, proceed carefully.",
                "If not, request fuel delivery."
            )
        ),
        ProblemRule(
            keywords = listOf("fuel gauge not working", "gauge broken", "fuel meter not working"),
            service = "fuel",
            severity = "minor",
            canSelfSolve = true,
            issueLabel = "Fuel gauge problem",
            selfSteps = listOf(
                "Estimate the last known fuel amount.",
                "Avoid long movement if fuel is uncertain.",
                "If the car begins losing power, request fuel delivery."
            )
        ),

        // ---------------- FUEL - PROVIDER ----------------
        ProblemRule(
            keywords = listOf("out of fuel", "no fuel", "fuel finished", "ran out of fuel"),
            service = "fuel",
            severity = "major",
            canSelfSolve = false,
            issueLabel = "Out of fuel"
        ),
        ProblemRule(
            keywords = listOf("wrong fuel", "petrol in diesel", "diesel in petrol"),
            service = "fuel",
            severity = "major",
            canSelfSolve = false,
            secondaryServices = listOf("garage", "towing"),
            issueLabel = "Wrong fuel used"
        ),

        // ---------------- TOWING - MINOR / SELF HELP ----------------
        ProblemRule(
            keywords = listOf("stuck in mud", "slightly stuck", "wheel stuck", "minor stuck"),
            service = "towing",
            severity = "minor",
            canSelfSolve = true,
            issueLabel = "Vehicle stuck lightly",
            selfSteps = listOf(
                "Do not over-accelerate.",
                "Remove loose obstruction if safe.",
                "Try gentle forward and backward movement.",
                "If the vehicle sinks deeper, request towing."
            )
        ),

        // ---------------- TOWING - PROVIDER ----------------
        ProblemRule(
            keywords = listOf("car not moving", "vehicle immobile", "cannot move", "cant move"),
            service = "towing",
            severity = "major",
            canSelfSolve = false,
            issueLabel = "Vehicle immobile"
        ),
        ProblemRule(
            keywords = listOf("severe accident", "bad accident", "crashed badly"),
            service = "towing",
            severity = "major",
            canSelfSolve = false,
            secondaryServices = listOf("ambulance"),
            issueLabel = "Severe accident"
        ),
        ProblemRule(
            keywords = listOf("overturned", "rolled over"),
            service = "towing",
            severity = "critical",
            canSelfSolve = false,
            emergency = true,
            secondaryServices = listOf("ambulance"),
            issueLabel = "Vehicle overturned"
        ),
        ProblemRule(
            keywords = listOf("deep ditch", "deep mud", "stuck in ditch"),
            service = "towing",
            severity = "major",
            canSelfSolve = false,
            issueLabel = "Vehicle badly stuck"
        ),
        ProblemRule(
            keywords = listOf("flood damaged", "burnt vehicle", "suspension broken", "axle broken"),
            service = "towing",
            severity = "major",
            canSelfSolve = false,
            issueLabel = "Vehicle not safe to move"
        ),

        // ---------------- AMBULANCE - MINOR / SELF HELP ----------------
        ProblemRule(
            keywords = listOf("minor cut", "small injury", "slight dizziness", "panic attack", "feeling unwell"),
            service = "ambulance",
            severity = "minor",
            canSelfSolve = true,
            issueLabel = "Minor medical issue",
            selfSteps = listOf(
                "Move to a safe area.",
                "Sit down and stay calm.",
                "Monitor symptoms closely.",
                "If symptoms worsen, request an ambulance immediately."
            )
        ),

        // ---------------- AMBULANCE - CRITICAL ----------------
        ProblemRule(
            keywords = listOf("unconscious", "passed out", "not breathing", "breathing difficulty", "difficulty breathing"),
            service = "ambulance",
            severity = "critical",
            canSelfSolve = false,
            emergency = true,
            issueLabel = "Critical medical emergency"
        ),
        ProblemRule(
            keywords = listOf("severe bleeding", "heavy bleeding", "blood loss"),
            service = "ambulance",
            severity = "critical",
            canSelfSolve = false,
            emergency = true,
            issueLabel = "Severe bleeding"
        ),
        ProblemRule(
            keywords = listOf("broken bone", "head injury", "heart attack", "stroke", "serious burns"),
            service = "ambulance",
            severity = "critical",
            canSelfSolve = false,
            emergency = true,
            issueLabel = "Serious medical emergency"
        ),
        ProblemRule(
            keywords = listOf("pregnant emergency", "labor emergency", "emergency labour", "emergency labor"),
            service = "ambulance",
            severity = "critical",
            canSelfSolve = false,
            emergency = true,
            issueLabel = "Emergency labor"
        )
    )

    fun classifyProblem(
        input: String,
        injury: Boolean,
        fire: Boolean,
        fuelLeak: Boolean,
        unsafeLocation: Boolean
    ): AutoAidDecision {
        val text = input.trim().lowercase()

        if (text.isBlank()) {
            return fallbackDecision()
        }

        // Highest priority: medical emergency
        if (injury || containsAny(text, listOf("bleeding", "unconscious", "not breathing", "broken bone", "head injury"))) {
            return AutoAidDecision(
                issue = "Possible human emergency",
                service = "ambulance",
                severity = "critical",
                canSelfSolve = false,
                needsProvider = true,
                emergency = true,
                selfSteps = emptyList(),
                providerReason = "Medical emergency detected.",
                userMessage = "This looks like a medical emergency. Request an ambulance immediately.",
                recommendation = ProviderRecommendation(
                    primary = "ambulance",
                    secondary = listOf("towing")
                )
            )
        }

        // Fire / fuel leak safety hazards
        if (fire || fuelLeak) {
            return AutoAidDecision(
                issue = "Vehicle safety hazard",
                service = "towing",
                severity = "critical",
                canSelfSolve = false,
                needsProvider = true,
                emergency = true,
                selfSteps = listOf(
                    "Move away from the vehicle if it is unsafe.",
                    "Do not smoke or create sparks.",
                    "Keep other people at a safe distance.",
                    "Request help immediately."
                ),
                providerReason = "Fire, smoke, or fuel leak makes the situation dangerous.",
                userMessage = "This situation is dangerous. Move to safety and request help immediately.",
                recommendation = ProviderRecommendation(
                    primary = if (injury) "ambulance" else "towing",
                    secondary = if (injury) listOf("towing") else emptyList()
                )
            )
        }

        val match = rules.firstOrNull { rule ->
            rule.keywords.any { keyword -> text.contains(keyword) }
        }

        if (match != null) {
            val needsProvider = !match.canSelfSolve || match.severity != "minor" || unsafeLocation
            val message = when {
                match.emergency -> "This situation looks critical. Get provider help immediately."
                match.canSelfSolve && !unsafeLocation -> "You can try a few safe self-help steps first."
                unsafeLocation -> "The location seems unsafe. It is better to request help."
                else -> "This likely needs a provider."
            }

            val providerReason = when {
                match.emergency -> "Emergency support is needed immediately."
                !match.canSelfSolve -> "This problem needs provider assistance."
                unsafeLocation -> "Even though self-help may be possible, the location is unsafe."
                else -> "Try the suggested self-help steps first."
            }

            return AutoAidDecision(
                issue = match.issueLabel ?: input,
                service = match.service,
                severity = match.severity,
                canSelfSolve = match.canSelfSolve && !unsafeLocation,
                needsProvider = needsProvider,
                emergency = match.emergency,
                selfSteps = if (match.canSelfSolve && !unsafeLocation) match.selfSteps else emptyList(),
                providerReason = providerReason,
                userMessage = message,
                recommendation = ProviderRecommendation(
                    primary = match.service,
                    secondary = match.secondaryServices
                )
            )
        }

        return fallbackDecision(input, unsafeLocation)
    }

    fun buildSelectableProviderTypes(decision: AutoAidDecision): List<String> {
        val combined = listOf(decision.recommendation.primary) + decision.recommendation.secondary
        return combined
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun fallbackDecision(
        input: String = "General vehicle problem",
        unsafeLocation: Boolean = false
    ): AutoAidDecision {
        val selfSteps = if (unsafeLocation) {
            emptyList()
        } else {
            listOf(
                "Park safely off the road.",
                "Turn on hazard lights.",
                "Inspect the vehicle carefully.",
                "If the problem worsens or the vehicle is unsafe, request provider help."
            )
        }

        return AutoAidDecision(
            issue = input,
            service = "garage",
            severity = "minor",
            canSelfSolve = !unsafeLocation,
            needsProvider = unsafeLocation,
            emergency = false,
            selfSteps = selfSteps,
            providerReason = if (unsafeLocation) {
                "The location is unsafe, so provider support is recommended."
            } else {
                "Defaulted to garage because no stronger match was found."
            },
            userMessage = if (unsafeLocation) {
                "This looks like a general vehicle issue, but the area seems unsafe. Request help."
            } else {
                "This looks like a general mechanical issue. Start with safe basic checks."
            },
            recommendation = ProviderRecommendation(
                primary = "garage",
                secondary = emptyList()
            )
        )
    }
}