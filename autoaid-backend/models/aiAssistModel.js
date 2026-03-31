export function createAiAnalyzeResponse({
  issue = "",
  serviceType = "garage",
  risk = "low",
  reason = "",
  action = "ASK_FOLLOW_UP",
  confidence = 0,
  userMessage = "",
  selfSolveAllowed = false,
  selfSolveSteps = [],
  recommendedService = "garage",
  summary = "",
  alternativeProviders = [],
  selectableProviders = [],
  providerReason = "",
  possibleProblems = [],
  followUpQuestions = [],
  meta = {},
}) {
  return {
    success: true,
    analysis: {
      issue,
      serviceType,
      risk,
      reason,
      action,
      confidence,
      userMessage,
    },
    selfSolve: {
      allowed: selfSolveAllowed,
      steps: selfSolveSteps,
    },
    providerBrief: {
      recommendedService,
      summary,
    },
    providerOptions: {
      primaryProvider: recommendedService,
      alternativeProviders,
      userSelectableProviders: selectableProviders,
      reason: providerReason,
    },
    possibleProblems,
    followUpQuestions,
    meta,
  };
}