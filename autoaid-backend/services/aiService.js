import OpenAI from "openai";

const client = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
});

const AUTOAID_AI_SYSTEM_PROMPT = `
You are AutoAid AI, a calm, practical, safety-first roadside and emergency assistant for the AutoAid platform.

Your role:
- Understand the user's problem in natural language.
- Reply clearly, naturally, and helpfully.
- Do not depend on hardcoded scripts.
- Keep responses concise, practical, and easy to understand.
- Ask follow-up questions only when truly useful.
- Prioritize safety.
- Avoid overly technical language unless necessary.
- When uncertain, use words like "might", "could", or "likely".

You can help with:
- vehicle breakdowns
- towing situations
- fuel problems
- accidents
- medical emergencies
- roadside uncertainty
- safety questions

Allowed service values:
- garage
- towing
- fuel
- ambulance
- none

Allowed problem_type values:
- vehicle_breakdown
- fuel_issue
- medical_emergency
- accident
- safety_issue
- general_question
- unknown

Allowed urgency values:
- low
- medium
- high
- critical

Allowed safe_to_drive values:
- yes
- no
- unknown

IMPORTANT DECISION RULES:

1. should_create_request = true ONLY IF:
   - the user clearly wants help now
   - OR the user is stranded / cannot continue
   - OR urgency is high or critical
   - OR the situation is dangerous
   - OR it is a medical emergency or accident

2. should_show_providers = true IF:
   - a service is likely needed
   - AND the user may want to choose a provider
   - AND the issue is not purely informational

3. If the user is only asking for advice or explanation:
   - should_create_request = false
   - should_show_providers = false

4. Service routing guidance:
   - garage: mechanical, battery, engine, tyre, repair-related issues
   - towing: vehicle cannot move, broken down on road, stranded vehicle
   - fuel: out of fuel, needs fuel delivery
   - ambulance: injury, unconsciousness, breathing trouble, labour, severe medical danger

5. Urgency guidance:
   - critical: life-threatening, unconscious, severe breathing trouble, major accident
   - high: stranded, cannot move, dangerous roadside situation
   - medium: needs help but not immediately life-threatening
   - low: mainly advice or uncertain issue

6. summary_for_provider:
   - short
   - clear
   - actionable
   - include key issue and urgency when relevant

Return ONLY JSON that matches the required schema.
`;

function safeArray(value) {
  return Array.isArray(value) ? value : [];
}

function normalizeHistory(history = []) {
  return safeArray(history)
    .map((m) => ({
      role: m?.role === "assistant" ? "assistant" : "user",
      content: String(m?.content || "").trim(),
    }))
    .filter((m) => m.content.length > 0);
}

function buildUserPrompt({ message, history, location, recentServices }) {
  const normalizedHistory = normalizeHistory(history);

  const historyText =
    normalizedHistory.length > 0
      ? normalizedHistory
          .map(
            (item) =>
              `${item.role === "assistant" ? "Assistant" : "User"}: ${item.content}`
          )
          .join("\n")
      : "No previous conversation.";

  const locationText = location
    ? `Location:
- label: ${location.label || "Unknown"}
- lat: ${location.lat ?? "Unknown"}
- lng: ${location.lng ?? "Unknown"}`
    : "Location: Unknown";

  const recentServicesText =
    safeArray(recentServices).length > 0
      ? `Recent services used: ${recentServices.join(", ")}`
      : "Recent services used: none";

  return `
${locationText}
${recentServicesText}

Previous conversation:
${historyText}

Latest user message:
${String(message || "").trim()}
`;
}

function fallbackAiResponse() {
  return {
    reply: "Sorry, I couldn’t process that properly right now.",
    problem_type: "unknown",
    likely_service: "none",
    urgency: "low",
    needs_follow_up: false,
    follow_up_question: "",
    safe_to_drive: "unknown",
    should_show_providers: false,
    should_create_request: false,
    summary_for_provider: "",
  };
}

function applyDecisionGuards(parsed = {}) {
  const result = {
    ...fallbackAiResponse(),
    ...parsed,
  };

  const urgency = String(result.urgency || "").toLowerCase();
  const likelyService = String(result.likely_service || "").toLowerCase();
  const problemType = String(result.problem_type || "").toLowerCase();

  if (urgency === "critical" || urgency === "high") {
    result.should_create_request = true;
  }

  if (
    ["medical_emergency", "accident", "safety_issue"].includes(problemType) &&
    likelyService !== "none"
  ) {
    result.should_create_request = true;
  }

  if (likelyService !== "none" && result.should_create_request !== true) {
    result.should_show_providers = true;
  }

  if (likelyService === "none") {
    result.should_show_providers = false;
    result.should_create_request = false;
  }

  if (!result.summary_for_provider || !String(result.summary_for_provider).trim()) {
    result.summary_for_provider = String(result.reply || "").trim();
  }

  return result;
}

export async function runAutoAidAI({
  message,
  history = [],
  location = null,
  recentServices = [],
}) {
  const cleanMessage = String(message || "").trim();

  if (!cleanMessage) {
    throw new Error("Message is required");
  }

  const inputText = buildUserPrompt({
    message: cleanMessage,
    history,
    location,
    recentServices,
  });

  const response = await client.responses.create({
    model: process.env.OPENAI_MODEL || "gpt-5.4",
    input: [
      {
        role: "system",
        content: [
          {
            type: "input_text",
            text: AUTOAID_AI_SYSTEM_PROMPT,
          },
        ],
      },
      {
        role: "user",
        content: [
          {
            type: "input_text",
            text: inputText,
          },
        ],
      },
    ],
    text: {
      format: {
        type: "json_schema",
        name: "autoaid_ai_response",
        strict: true,
        schema: {
          type: "object",
          additionalProperties: false,
          properties: {
            reply: { type: "string" },
            problem_type: {
              type: "string",
              enum: [
                "vehicle_breakdown",
                "fuel_issue",
                "medical_emergency",
                "accident",
                "safety_issue",
                "general_question",
                "unknown",
              ],
            },
            likely_service: {
              type: "string",
              enum: ["garage", "towing", "fuel", "ambulance", "none"],
            },
            urgency: {
              type: "string",
              enum: ["low", "medium", "high", "critical"],
            },
            needs_follow_up: { type: "boolean" },
            follow_up_question: { type: "string" },
            safe_to_drive: {
              type: "string",
              enum: ["yes", "no", "unknown"],
            },
            should_show_providers: { type: "boolean" },
            should_create_request: { type: "boolean" },
            summary_for_provider: { type: "string" },
          },
          required: [
            "reply",
            "problem_type",
            "likely_service",
            "urgency",
            "needs_follow_up",
            "follow_up_question",
            "safe_to_drive",
            "should_show_providers",
            "should_create_request",
            "summary_for_provider",
          ],
        },
      },
    },
  });

  const raw = response.output_text || "{}";

  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch {
    parsed = fallbackAiResponse();
  }

  return applyDecisionGuards(parsed);
}