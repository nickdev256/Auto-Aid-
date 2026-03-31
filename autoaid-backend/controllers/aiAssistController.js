import { analyzeAutoAidProblem } from "../services/aiDecisionEngine.js";

function normalizeText(value) {
  return typeof value === "string" ? value.trim() : "";
}

function normalizeFlags(flags = {}) {
  return {
    injury: Boolean(flags?.injury),
    fire: Boolean(flags?.fire),
    fuelLeak: Boolean(flags?.fuelLeak),
    unsafeLocation: Boolean(flags?.unsafeLocation),
  };
}

function normalizeVehicle(vehicle = {}) {
  return {
    type: normalizeText(vehicle?.type) || null,
    fuelType: normalizeText(vehicle?.fuelType) || null,
  };
}

function normalizeLocation(location = null) {
  if (!location || typeof location !== "object") return null;

  const lat =
    typeof location.lat === "number" && Number.isFinite(location.lat)
      ? location.lat
      : null;

  const lng =
    typeof location.lng === "number" && Number.isFinite(location.lng)
      ? location.lng
      : null;

  const address = normalizeText(location?.address) || null;

  if (lat == null && lng == null && !address) return null;

  return { lat, lng, address };
}

function normalizePreviousAnalysis(previousAnalysis = null) {
  if (!previousAnalysis || typeof previousAnalysis !== "object") return null;
  return previousAnalysis;
}

export async function analyzeProblem(req, res) {
  try {
    const body = req.body || {};

    const rawMessage = normalizeText(body.message);
    if (!rawMessage) {
      return res.status(400).json({
        success: false,
        message: "Problem description is required.",
      });
    }

    const flags = normalizeFlags(body.flags);
    const vehicle = normalizeVehicle(body.vehicle);
    const location = normalizeLocation(body.location);
    const previousAnalysis = normalizePreviousAnalysis(body.previousAnalysis);
    const stage = normalizeText(body.stage || "chat") || "chat";

    const result = await analyzeAutoAidProblem({
      message: rawMessage,
      rawMessage,
      flags,
      vehicle,
      location,
      stage,
      previousAnalysis,
    });

    return res.status(200).json(result);
  } catch (error) {
    console.error("AI analyze problem error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to analyze problem.",
      error: error.message,
    });
  }
}

export async function escalateAfterFailedSelfSolve(req, res) {
  try {
    const { originalAnalysis, message, location, vehicle } = req.body || {};

    if (!originalAnalysis || typeof originalAnalysis !== "object") {
      return res.status(400).json({
        success: false,
        message: "originalAnalysis is required.",
      });
    }

    const analysis = originalAnalysis.analysis || {};
    const providerBrief = originalAnalysis.providerBrief || {};
    const providerOptions = originalAnalysis.providerOptions || {};

    const risk = normalizeText(analysis.risk).toLowerCase() || "low";
    const serviceType =
      normalizeText(providerOptions.primaryProvider).toLowerCase() ||
      normalizeText(providerBrief.recommendedService).toLowerCase() ||
      normalizeText(analysis.serviceType).toLowerCase() ||
      "garage";

    const alternativeProviders = Array.isArray(providerOptions.alternativeProviders)
      ? providerOptions.alternativeProviders.filter(Boolean)
      : [];

    const selectableProviders = Array.isArray(providerOptions.userSelectableProviders)
      ? providerOptions.userSelectableProviders.filter(Boolean)
      : [serviceType, ...alternativeProviders].filter(Boolean);

    return res.status(200).json({
      success: true,
      message: "Escalated to provider flow successfully.",
      providerPayload: {
        action: "PROVIDER_REQUIRED",
        recommendedService: serviceType,
        alternativeProviders,
        selectableProviders: [...new Set(selectableProviders)],
        priority: ["high", "critical", "emergency"].includes(risk) ? "high" : "normal",
        location: normalizeLocation(location),
        vehicle: normalizeVehicle(vehicle),
        aiSummary:
          normalizeText(providerBrief.summary) ||
          normalizeText(analysis.reason) ||
          normalizeText(analysis.userMessage) ||
          "User still needs professional help.",
        message:
          normalizeText(message) ||
          "User attempted self-solve steps but the issue was not resolved.",
      },
    });
  } catch (error) {
    console.error("AI escalation error:", error);
    return res.status(500).json({
      success: false,
      message: "Failed to escalate to provider flow.",
      error: error.message,
    });
  }
}