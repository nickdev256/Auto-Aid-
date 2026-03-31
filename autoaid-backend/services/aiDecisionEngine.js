function cleanText(value) {
  return typeof value === "string" ? value.trim() : "";
}

function lower(value) {
  return cleanText(value).toLowerCase();
}

function containsAny(text, keywords = []) {
  return keywords.some((keyword) => text.includes(keyword));
}

function uniq(values = []) {
  return [...new Set(values.filter(Boolean).map((v) => lower(v)))];
}

function isAffirmative(text) {
  return containsAny(text, [
    "yes",
    "yeah",
    "yap",
    "worked",
    "it worked",
    "fixed",
    "resolved",
    "okay now",
    "ok now",
    "fine now",
    "solved",
  ]);
}

function isNegative(text) {
  return containsAny(text, [
    "no",
    "not",
    "didn't work",
    "didnt work",
    "failed",
    "not fixed",
    "still there",
    "still not working",
    "not working",
    "worse",
    "got worse",
    "cannot move",
    "cant move",
    "no spare",
    "still overheating",
    "still stuck",
    "still not starting",
  ]);
}

const PROBLEM_RULES = [
  {
    keywords: ["flat tyre", "flat tire", "puncture", "tyre puncture", "tire puncture"],
    issue: "Tyre or wheel issue",
    serviceType: "garage",
    risk: "low",
    action: "SELF_SOLVE_FIRST",
    selfSolve: {
      allowed: true,
      steps: [
        "Park in a safe place away from traffic.",
        "Turn on hazard lights.",
        "Inspect the tyre carefully.",
        "Use the spare tyre if available and safe.",
        "If not safe or no spare, tell me the steps did not work."
      ],
    },
    providerOptions: {
      primaryProvider: "garage",
      alternativeProviders: ["towing"],
      reason: "A tyre issue usually needs garage help first, but towing may help if the vehicle cannot move safely.",
    },
    userMessage:
      "This looks like a tyre or wheel issue. Please try these safe steps first.",
    possibleProblems: ["Flat tyre", "Tyre puncture", "Wheel issue"],
  },
  {
    keywords: ["dead battery", "battery dead", "weak battery", "battery low"],
    issue: "Battery problem",
    serviceType: "garage",
    risk: "low",
    action: "SELF_SOLVE_FIRST",
    selfSolve: {
      allowed: true,
      steps: [
        "Check whether dashboard lights come on.",
        "Check battery terminals for looseness.",
        "Try a jump start if available and safe.",
        "If the car still does not start, tell me the steps did not work."
      ],
    },
    providerOptions: {
      primaryProvider: "garage",
      alternativeProviders: ["towing"],
      reason: "Battery issues often start with garage support, but towing may be needed if the vehicle stays immobile.",
    },
    userMessage:
      "This looks like a battery-related problem. Please try these safe steps first.",
    possibleProblems: ["Dead battery", "Weak battery"],
  },
  {
    keywords: ["car won't start", "car wont start", "vehicle won't start", "vehicle wont start", "starter delay"],
    issue: "Starting problem",
    serviceType: "garage",
    risk: "low",
    action: "SELF_SOLVE_FIRST",
    selfSolve: {
      allowed: true,
      steps: [
        "Ensure the car is in park or neutral.",
        "Check if the battery seems weak.",
        "Try restarting once or twice only.",
        "If it still fails, tell me the steps did not work."
      ],
    },
    providerOptions: {
      primaryProvider: "garage",
      alternativeProviders: ["towing"],
      reason: "A starting problem often begins with garage help, but towing may be needed if the vehicle remains immobile.",
    },
    userMessage:
      "This looks like a starting problem. Please try these checks first.",
    possibleProblems: ["Starting problem", "Battery issue"],
  },
  {
    keywords: ["overheating", "engine hot", "temperature high", "car overheating"],
    issue: "Engine overheating",
    serviceType: "garage",
    risk: "medium",
    action: "SELF_SOLVE_FIRST",
    selfSolve: {
      allowed: true,
      steps: [
        "Stop the vehicle in a safe place.",
        "Turn off the engine.",
        "Do not open the radiator cap immediately.",
        "Let the engine cool down first.",
        "If overheating continues, tell me the steps did not work."
      ],
    },
    providerOptions: {
      primaryProvider: "garage",
      alternativeProviders: ["towing"],
      reason: "Overheating may begin as a garage issue, but towing is safer if the vehicle cannot continue.",
    },
    userMessage:
      "The vehicle may be overheating. Please try these safe steps first.",
    possibleProblems: ["Engine overheating", "Cooling system issue"],
  },
  {
    keywords: ["low fuel", "fuel almost finished", "forgot to refuel", "almost no fuel"],
    issue: "Low fuel",
    serviceType: "fuel",
    risk: "low",
    action: "SELF_SOLVE_FIRST",
    selfSolve: {
      allowed: true,
      steps: [
        "Check the fuel level.",
        "Reduce unnecessary movement.",
        "If a fuel station is very near and safe, proceed carefully.",
        "If not, tell me the steps did not work."
      ],
    },
    providerOptions: {
      primaryProvider: "fuel",
      alternativeProviders: [],
      reason: "Low fuel can sometimes be handled by the user, but fuel delivery is the right provider service if needed.",
    },
    userMessage:
      "This looks like a fuel issue. Please try these safe steps first.",
    possibleProblems: ["Low fuel", "Fuel nearly finished"],
  },
  {
    keywords: ["out of fuel", "no fuel", "fuel finished", "ran out of fuel"],
    issue: "Out of fuel",
    serviceType: "fuel",
    risk: "medium",
    action: "PROVIDER_REQUIRED",
    selfSolve: { allowed: false, steps: [] },
    providerOptions: {
      primaryProvider: "fuel",
      alternativeProviders: [],
      reason: "The vehicle needs fuel delivery support.",
    },
    userMessage:
      "The vehicle appears to be out of fuel. I recommend fuel assistance now.",
    possibleProblems: ["Out of fuel"],
  },
  {
    keywords: ["wrong fuel", "petrol in diesel", "diesel in petrol"],
    issue: "Wrong fuel in vehicle",
    serviceType: "fuel",
    risk: "high",
    action: "PROVIDER_REQUIRED",
    selfSolve: { allowed: false, steps: [] },
    providerOptions: {
      primaryProvider: "fuel",
      alternativeProviders: ["garage", "towing"],
      reason: "Wrong fuel can damage the engine and may need draining, garage support, or towing.",
    },
    userMessage:
      "Wrong fuel in the vehicle can become serious. Professional help is recommended immediately.",
    possibleProblems: ["Wrong fuel used"],
  },
  {
    keywords: ["stuck in mud", "slightly stuck", "wheel stuck", "minor stuck"],
    issue: "Vehicle stuck",
    serviceType: "towing",
    risk: "medium",
    action: "SELF_SOLVE_FIRST",
    selfSolve: {
      allowed: true,
      steps: [
        "Avoid over-accelerating.",
        "Remove loose obstruction if safe.",
        "Try gentle forward and backward movement.",
        "If it still does not move, tell me the steps did not work."
      ],
    },
    providerOptions: {
      primaryProvider: "towing",
      alternativeProviders: [],
      reason: "The vehicle may need towing if it cannot free itself safely.",
    },
    userMessage:
      "The vehicle seems stuck. Please try these safe recovery steps first.",
    possibleProblems: ["Vehicle stuck", "Wheel stuck"],
  },
  {
    keywords: ["car not moving", "vehicle immobile", "cannot move", "cant move"],
    issue: "Vehicle immobile",
    serviceType: "towing",
    risk: "high",
    action: "PROVIDER_REQUIRED",
    selfSolve: { allowed: false, steps: [] },
    providerOptions: {
      primaryProvider: "towing",
      alternativeProviders: ["garage"],
      reason: "A vehicle that cannot move safely usually needs towing first.",
    },
    userMessage:
      "Since the vehicle cannot move, I recommend towing support now.",
    possibleProblems: ["Vehicle immobile"],
  },
  {
    keywords: ["severe accident", "bad accident", "crashed badly", "overturned", "rolled over"],
    issue: "Severe vehicle damage",
    serviceType: "towing",
    risk: "high",
    action: "PROVIDER_REQUIRED",
    selfSolve: { allowed: false, steps: [] },
    providerOptions: {
      primaryProvider: "towing",
      alternativeProviders: ["ambulance"],
      reason: "A badly damaged vehicle often needs towing, and ambulance support may also be needed if anyone is injured.",
    },
    userMessage:
      "This sounds like a serious incident. Towing support is strongly recommended.",
    possibleProblems: ["Severe accident", "Vehicle overturned", "Major damage"],
  },
  {
    keywords: ["minor cut", "small injury", "slight dizziness", "panic attack", "feeling unwell"],
    issue: "Minor medical concern",
    serviceType: "ambulance",
    risk: "medium",
    action: "SELF_SOLVE_FIRST",
    selfSolve: {
      allowed: true,
      steps: [
        "Move to a safe place.",
        "Sit down and stay calm.",
        "Monitor symptoms closely.",
        "If symptoms worsen, tell me the steps did not work."
      ],
    },
    providerOptions: {
      primaryProvider: "ambulance",
      alternativeProviders: [],
      reason: "Symptoms appear mild for now, but they should be monitored closely.",
    },
    userMessage:
      "This may be a mild medical issue. Please follow these steps and tell me whether they worked.",
    possibleProblems: ["Minor medical issue"],
  },
  {
    keywords: [
      "unconscious",
      "passed out",
      "not breathing",
      "breathing difficulty",
      "difficulty breathing",
      "severe bleeding",
      "heavy bleeding",
      "blood loss",
      "broken bone",
      "head injury",
      "heart attack",
      "stroke",
      "serious burns",
      "pregnant emergency",
      "labor emergency",
      "emergency labour",
      "emergency labor",
    ],
    issue: "Medical emergency",
    serviceType: "ambulance",
    risk: "critical",
    action: "EMERGENCY",
    selfSolve: { allowed: false, steps: [] },
    providerOptions: {
      primaryProvider: "ambulance",
      alternativeProviders: ["towing"],
      reason: "This situation looks like a human emergency and needs ambulance support immediately.",
    },
    userMessage:
      "This looks like a medical emergency. Request ambulance help immediately.",
    possibleProblems: ["Medical emergency"],
  },
];

function buildAiResponse({
  issue,
  serviceType,
  risk,
  reason,
  action,
  confidence,
  userMessage,
  selfSolve,
  providerOptions,
  possibleProblems = [],
  followUpQuestions = [],
  rawMessage = "",
  vehicle = {},
  location = null,
  stage = "chat",
  source = "rule_match",
}) {
  const primaryProvider = providerOptions.primaryProvider;
  const alternativeProviders = providerOptions.alternativeProviders || [];
  const selectable = providerOptions.userSelectableProviders || uniq([primaryProvider, ...alternativeProviders]);

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
    selfSolve,
    providerBrief: {
      recommendedService: primaryProvider,
      summary: rawMessage || issue,
    },
    providerOptions: {
      primaryProvider,
      alternativeProviders,
      userSelectableProviders: selectable,
      reason: providerOptions.reason,
    },
    possibleProblems,
    followUpQuestions,
    meta: {
      vehicle,
      location,
      stage,
      source,
    },
  };
}

function buildFollowUpQuestion(issue) {
  return [`Did the steps work for the ${issue.toLowerCase()}? Reply yes or no.`];
}

function buildEmergencyFromFlags({ flags, vehicle, location, rawMessage }) {
  const issue = flags.injury
    ? "Possible human emergency"
    : "Vehicle safety hazard";

  const primaryProvider = flags.injury ? "ambulance" : "towing";
  const secondary = flags.injury ? ["towing"] : [];

  return buildAiResponse({
    issue,
    serviceType: primaryProvider,
    risk: "critical",
    reason: flags.injury
      ? "An injury was reported, so medical assistance takes priority."
      : "Fire, smoke, or fuel leakage makes the situation dangerous.",
    action: flags.injury ? "EMERGENCY" : "PROVIDER_REQUIRED",
    confidence: 0.98,
    userMessage: flags.injury
      ? "This looks like a medical emergency. Request ambulance support immediately."
      : "This looks dangerous. Move to safety and request help immediately.",
    selfSolve: { allowed: false, steps: [] },
    providerOptions: {
      primaryProvider,
      alternativeProviders: secondary,
      userSelectableProviders: uniq([primaryProvider, ...secondary]),
      reason: "Emergency conditions were detected from the safety flags.",
    },
    possibleProblems: [issue],
    followUpQuestions: [],
    rawMessage,
    vehicle,
    location,
    source: "flags_priority",
  });
}

function matchProblem(rawMessage = "") {
  const text = lower(rawMessage);
  return PROBLEM_RULES.find((rule) => containsAny(text, rule.keywords)) || null;
}

function buildWorkedSuccessResponse({ previousAnalysis, rawMessage, vehicle, location, stage }) {
  const prevIssue = previousAnalysis?.analysis?.issue || "issue";
  const prevPrimary =
    previousAnalysis?.providerOptions?.primaryProvider ||
    previousAnalysis?.analysis?.serviceType ||
    "garage";

  return buildAiResponse({
    issue: prevIssue,
    serviceType: prevPrimary,
    risk: "low",
    reason: "The user confirmed the self-solve steps worked.",
    action: "RESOLVED",
    confidence: 0.95,
    userMessage: "Great. I’m glad the steps worked. You can continue carefully and monitor the situation.",
    selfSolve: { allowed: false, steps: [] },
    providerOptions: {
      primaryProvider: prevPrimary,
      alternativeProviders: previousAnalysis?.providerOptions?.alternativeProviders || [],
      userSelectableProviders: previousAnalysis?.providerOptions?.userSelectableProviders || [prevPrimary],
      reason: "The issue appears resolved for now.",
    },
    possibleProblems: [prevIssue],
    followUpQuestions: [],
    rawMessage,
    vehicle,
    location,
    stage,
    source: "previous_context_worked",
  });
}

function buildNotWorkedResponse({ previousAnalysis, rawMessage, vehicle, location, stage }) {
  const prevIssue = previousAnalysis?.analysis?.issue || "Vehicle issue";
  const prevPrimary =
    previousAnalysis?.providerOptions?.primaryProvider ||
    previousAnalysis?.analysis?.serviceType ||
    "garage";
  const alternatives = previousAnalysis?.providerOptions?.alternativeProviders || [];

  let risk = lower(previousAnalysis?.analysis?.risk || "medium");
  if (risk === "low") risk = "medium";

  let message = "Since the steps did not work, I recommend requesting provider assistance now.";

  if (prevIssue.toLowerCase().includes("tyre") || rawMessage.toLowerCase().includes("no spare")) {
    message =
      "Since the steps did not work and you do not have a spare, I recommend requesting garage assistance now. Towing is also possible if the vehicle is not safe to move.";
  } else if (prevIssue.toLowerCase().includes("starting") || prevIssue.toLowerCase().includes("battery")) {
    message =
      "Since the vehicle still is not starting, I recommend requesting provider assistance now.";
  } else if (prevIssue.toLowerCase().includes("overheat")) {
    message =
      "Since the overheating is still there, I recommend provider help now. Do not continue driving if the vehicle is unsafe.";
  } else if (prevIssue.toLowerCase().includes("stuck") || rawMessage.toLowerCase().includes("cannot move")) {
    message =
      "Since the vehicle still cannot move, I recommend towing support now.";
  }

  return buildAiResponse({
    issue: prevIssue,
    serviceType: prevPrimary,
    risk,
    reason: "The user confirmed that the self-solve steps did not resolve the issue.",
    action: "PROVIDER_REQUIRED",
    confidence: 0.93,
    userMessage: message,
    selfSolve: { allowed: false, steps: [] },
    providerOptions: {
      primaryProvider: prevPrimary,
      alternativeProviders: alternatives,
      userSelectableProviders: uniq([prevPrimary, ...alternatives]),
      reason: "The previous self-solve attempt did not resolve the problem.",
    },
    possibleProblems: [prevIssue],
    followUpQuestions: [],
    rawMessage,
    vehicle,
    location,
    stage,
    source: "previous_context_failed",
  });
}

function buildFollowUpFromPrevious({
  rawMessage,
  previousAnalysis,
  vehicle,
  location,
  stage,
}) {
  if (!previousAnalysis || typeof previousAnalysis !== "object") return null;

  const prevAction = lower(previousAnalysis?.analysis?.action || "");
  const prevSelfSolveAllowed = Boolean(previousAnalysis?.selfSolve?.allowed);

  if (!(prevSelfSolveAllowed || prevAction === "self_solve_first")) {
    return null;
  }

  const text = lower(rawMessage);

  if (isAffirmative(text)) {
    return buildWorkedSuccessResponse({
      previousAnalysis,
      rawMessage,
      vehicle,
      location,
      stage,
    });
  }

  if (isNegative(text)) {
    return buildNotWorkedResponse({
      previousAnalysis,
      rawMessage,
      vehicle,
      location,
      stage,
    });
  }

  return {
    success: true,
    analysis: {
      issue: previousAnalysis?.analysis?.issue || "Vehicle issue",
      serviceType:
        previousAnalysis?.analysis?.serviceType ||
        previousAnalysis?.providerOptions?.primaryProvider ||
        "garage",
      risk: previousAnalysis?.analysis?.risk || "low",
      reason: "The AI is waiting for confirmation on whether the self-solve steps worked.",
      action: "ASK_IF_WORKED",
      confidence: 0.9,
      userMessage: "Please tell me whether the steps worked or not.",
    },
    selfSolve: previousAnalysis?.selfSolve || { allowed: false, steps: [] },
    providerBrief: previousAnalysis?.providerBrief || {
      recommendedService:
        previousAnalysis?.providerOptions?.primaryProvider || "garage",
      summary: previousAnalysis?.analysis?.issue || "Vehicle issue",
    },
    providerOptions: previousAnalysis?.providerOptions || {
      primaryProvider: "garage",
      alternativeProviders: [],
      userSelectableProviders: ["garage"],
      reason: "Awaiting self-solve result.",
    },
    possibleProblems: previousAnalysis?.possibleProblems || [],
    followUpQuestions: buildFollowUpQuestion(previousAnalysis?.analysis?.issue || "issue"),
    meta: {
      vehicle,
      location,
      stage,
      source: "ask_if_worked",
    },
  };
}

function buildFallback({ rawMessage, vehicle, location, stage = "chat" }) {
  return buildAiResponse({
    issue: "General vehicle issue",
    serviceType: "garage",
    risk: "low",
    reason: "No stronger match was found, so this was treated as a general mechanical problem.",
    action: "ASK_FOLLOW_UP",
    confidence: 0.62,
    userMessage:
      "I understand there is a vehicle issue. Tell me exactly what the vehicle is doing so I can guide you better.",
    selfSolve: {
      allowed: true,
      steps: [
        "Park safely off the road.",
        "Turn on hazard lights.",
        "Inspect the vehicle carefully.",
        "If the vehicle is unsafe, stop and request help."
      ],
    },
    providerOptions: {
      primaryProvider: "garage",
      alternativeProviders: ["towing"],
      userSelectableProviders: ["garage", "towing"],
      reason: "Most uncategorized vehicle issues start with garage support.",
    },
    possibleProblems: ["General mechanical issue", "Vehicle fault"],
    followUpQuestions: [
      "Is the vehicle still moving normally, or has it stopped completely?"
    ],
    rawMessage,
    vehicle,
    location,
    stage,
    source: "fallback",
  });
}

export async function analyzeAutoAidProblem({
  message,
  rawMessage,
  flags = {},
  vehicle = {},
  location = null,
  stage = "chat",
  previousAnalysis = null,
}) {
  if (flags.injury || flags.fire || flags.fuelLeak) {
    return buildEmergencyFromFlags({ flags, vehicle, location, rawMessage });
  }

  const previousFollowUp = buildFollowUpFromPrevious({
    rawMessage,
    previousAnalysis,
    vehicle,
    location,
    stage,
  });

  if (previousFollowUp) {
    return previousFollowUp;
  }

  const matched = matchProblem(rawMessage || message || "");

  if (matched) {
    let risk = matched.risk;
    let action = matched.action;
    let allowed = matched.selfSolve.allowed;
    let steps = matched.selfSolve.steps;

    if (flags.unsafeLocation && allowed) {
      risk = risk === "low" ? "medium" : risk;
      action = "PROVIDER_REQUIRED";
      allowed = false;
      steps = [];
    }

    const response = buildAiResponse({
      issue: matched.issue,
      serviceType: matched.serviceType,
      risk,
      reason: matched.providerOptions.reason,
      action,
      confidence:
        risk === "critical"
          ? 0.98
          : risk === "high"
          ? 0.92
          : risk === "medium"
          ? 0.84
          : 0.76,
      userMessage:
        flags.unsafeLocation && matched.selfSolve.allowed
          ? "The location seems unsafe, so it is better to get provider help now."
          : matched.userMessage,
      selfSolve: {
        allowed,
        steps,
      },
      providerOptions: {
        primaryProvider: matched.providerOptions.primaryProvider,
        alternativeProviders: matched.providerOptions.alternativeProviders || [],
        userSelectableProviders: uniq([
          matched.providerOptions.primaryProvider,
          ...(matched.providerOptions.alternativeProviders || []),
        ]),
        reason: matched.providerOptions.reason,
      },
      possibleProblems: matched.possibleProblems || [],
      followUpQuestions:
        allowed && steps.length
          ? buildFollowUpQuestion(matched.issue)
          : [],
      rawMessage,
      vehicle,
      location,
      stage,
      source: "rule_match",
    });

    return response;
  }

  return buildFallback({ rawMessage, vehicle, location, stage });
}