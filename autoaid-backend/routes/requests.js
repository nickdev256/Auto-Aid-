import express from "express";
import mongoose from "mongoose";
import Request from "../models/Request.js";
import User from "../models/User.js";
import ProviderWallet from "../models/ProviderWallet.js";
import SystemWallet from "../models/SystemWallet.js";
import { protect } from "../middleware/authMiddleware.js";

const router = express.Router();

/* -----------------------------
   Helper: validate ObjectId
----------------------------- */
function ensureObjectId(id) {
  return mongoose.Types.ObjectId.isValid(id);
}

/* -----------------------------
   Detect platform from header
----------------------------- */
function getClient(req) {
  const x = (req.headers["x-client"] || "").toString().toLowerCase();
  return x === "android" ? "android" : "web";
}

/* -----------------------------
   System fee calculator
   5% of total amount
----------------------------- */
function calculateSystemFee(totalAmount) {
  return Number((Number(totalAmount || 0) * 0.05).toFixed(2));
}

/* -----------------------------
   Emit request events
----------------------------- */
function emitRequestEvent(req, eventName, requestDoc) {
  const io = req.app.get("io");
  if (!io) return;

  const providerType = String(requestDoc.providerType || "").trim().toLowerCase();
  const requestId = String(requestDoc._id || requestDoc.id || "");

  if (providerType) {
    io.to(`type:${providerType}`).emit(eventName, requestDoc);
  }

  if (requestDoc.targetProviderId) {
    io.to(`provider:${String(requestDoc.targetProviderId)}`).emit(eventName, requestDoc);
  }

  if (requestDoc.assignedProviderId) {
    io.to(`provider:${String(requestDoc.assignedProviderId)}`).emit(eventName, requestDoc);
  }

  if (requestDoc.userId) {
    io.to(`user:${String(requestDoc.userId)}`).emit(eventName, requestDoc);
  }

  if (requestId) {
    io.to(`request:${requestId}`).emit(eventName, requestDoc);
  }
}

/* -----------------------------
   Broadcast request to providers
----------------------------- */
async function broadcastRequestToProviders(req, requestDoc) {
  const io = req.app.get("io");
  if (!io) return;

  const providerType = String(requestDoc.providerType || "").trim().toLowerCase();
  let providers = [];

  if (requestDoc.targetProviderId) {
    providers = await User.find({
      _id: requestDoc.targetProviderId,
      role: "provider",
      isApprovedProvider: true,
      isAvailable: true,
      isOnline: true,
      businessType: providerType,
    }).select("_id name businessType socketId");
  } else {
    providers = await User.find({
      role: "provider",
      businessType: providerType,
      isApprovedProvider: true,
      isAvailable: true,
      isOnline: true,
      status: { $in: ["approved", "active"] },
    }).select("_id name businessType socketId");
  }

  for (const provider of providers) {
    io.to(`provider:${String(provider._id)}`).emit("new_request_broadcast", {
      requestId: String(requestDoc._id),
      request: requestDoc,
      service: requestDoc.service,
      providerType: requestDoc.providerType,
      userLocation: requestDoc.userLocation || { lat: 0, lng: 0 },
      createdAt: requestDoc.createdAt || new Date(),
    });

    io.to(`provider:${String(provider._id)}`).emit("notify", {
      id: `req_${requestDoc._id}_${provider._id}`,
      type: "request",
      title: "New service request",
      body: `${requestDoc.service || requestDoc.providerType} request`,
      requestId: String(requestDoc._id),
      createdAt: new Date().toISOString(),
    });
  }
}

/* -----------------------------
   Status helpers
----------------------------- */
const REQUEST_STATUSES = [
  "pending",
  "assigned",
  "arrived",
  "quoted",
  "awaiting_payment",
  "in_progress",
  "awaiting_dual_confirmation",
  "completed",
  "cancelled",
];

function normalizeStatus(status) {
  const value = String(status || "").trim().toLowerCase();

  switch (value) {
    case "request_sent":
      return "pending";
    case "provider_assigned":
    case "driver_assigned":
    case "mechanic_assigned":
    case "vendor_assigned":
      return "assigned";
    case "ongoing":
    case "on going":
      return "in_progress";
    case "canceled":
      return "cancelled";
    default:
      return value;
  }
}

function isValidStatus(status) {
  return REQUEST_STATUSES.includes(normalizeStatus(status));
}

function canTransitionStatus(currentStatus, nextStatus) {
  const current = normalizeStatus(currentStatus);
  const next = normalizeStatus(nextStatus);

  const transitions = {
    pending: ["assigned", "cancelled"],
    assigned: ["arrived", "cancelled", "awaiting_payment", "in_progress"],
    arrived: ["quoted", "awaiting_payment", "in_progress", "cancelled"],
    quoted: ["awaiting_payment", "in_progress", "cancelled"],
    awaiting_payment: ["in_progress", "cancelled"],
    in_progress: ["awaiting_dual_confirmation", "completed", "cancelled"],
    awaiting_dual_confirmation: ["completed", "cancelled"],
    completed: [],
    cancelled: [],
  };

  return transitions[current]?.includes(next) || false;
}

function pushStatusHistory(request, status, changedBy, note = "") {
  const normalized = normalizeStatus(status);

  if (!Array.isArray(request.statusHistory)) {
    request.statusHistory = [];
  }

  request.statusHistory.push({
    status: normalized,
    changedAt: new Date(),
    changedBy: changedBy ? new mongoose.Types.ObjectId(changedBy) : undefined,
    note,
  });
}

/* -----------------------------
   Role helpers
----------------------------- */
function isAdmin(user) {
  return String(user?.role || "").toLowerCase() === "admin";
}

function isProvider(user) {
  const role = String(user?.role || "").toLowerCase();
  return ["provider", "garage", "towing", "fuel", "ambulance", "mechanic"].includes(role);
}

function isRequestOwner(request, userId) {
  return String(request.userId) === String(userId);
}

function isAssignedProvider(request, userId) {
  return String(request.assignedProviderId || "") === String(userId);
}

/* -----------------------------
   Escrow release helper
----------------------------- */
async function releaseEscrowPayment(request) {
  if (!request) {
    throw new Error("Request not found");
  }

  if (request.paymentStatus === "released") {
    return {
      ok: false,
      message: "Payment already released",
      request,
    };
  }

  if (request.paymentStatus !== "held_in_escrow") {
    return {
      ok: false,
      message: "Payment is not held in escrow",
      request,
    };
  }

  if (!request.providerCompleted || !request.userCompleted) {
    return {
      ok: false,
      message: "Both provider and user must complete first",
      request,
    };
  }

  const totalAmount = Number(request.totalAmount || 0);
  if (totalAmount <= 0) {
    throw new Error("Invalid total amount");
  }

  const systemFee = calculateSystemFee(totalAmount);
  const providerAmount = Number((totalAmount - systemFee).toFixed(2));

  let providerWallet = await ProviderWallet.findOne({
    providerId: request.assignedProviderId,
  });

  if (!providerWallet) {
    providerWallet = await ProviderWallet.create({
      providerId: request.assignedProviderId,
      availableBalance: 0,
      pendingBalance: 0,
      totalEarned: 0,
      totalWithdrawn: 0,
    });
  }

  let systemWallet = await SystemWallet.findOne();
  if (!systemWallet) {
    systemWallet = await SystemWallet.create({
      totalSystemFees: 0,
      totalSubscriptionRevenue: 0,
      totalRevenue: 0,
    });
  }

  providerWallet.availableBalance += providerAmount;
  providerWallet.totalEarned += providerAmount;

  systemWallet.totalSystemFees += systemFee;
  systemWallet.totalRevenue =
    Number(systemWallet.totalSystemFees || 0) +
    Number(systemWallet.totalSubscriptionRevenue || 0);

  request.providerAmount = providerAmount;
  request.systemFee = systemFee;
  request.paymentStatus = "released";
  request.status = "completed";
  request.completedAt = new Date();

  await providerWallet.save();
  await systemWallet.save();
  await request.save();

  if (request.assignedProviderId) {
    await User.findByIdAndUpdate(request.assignedProviderId, {
      $set: { currentRequestId: null },
    });
  }

  return {
    ok: true,
    message: "Payment released successfully",
    request,
    providerWallet,
    systemWallet,
  };
}

/**
 * POST /api/requests
 * Create request (user role)
 * body may include targetProviderId (optional)
 */
router.post("/", protect, async (req, res) => {
  try {
    const body = req.body || {};

    if (!body.providerType) {
      return res.status(400).json({ message: "providerType is required" });
    }

    if (!body.service) {
      return res.status(400).json({ message: "service is required" });
    }

    body.providerType = String(body.providerType).trim().toLowerCase();
    body.service = String(body.service).trim().toLowerCase();

    const rawTarget = body.targetProviderId;
    if (rawTarget === "" || rawTarget === null || rawTarget === undefined) {
      delete body.targetProviderId;
    } else {
      const targetStr = String(rawTarget).trim();
      if (!ensureObjectId(targetStr)) {
        return res.status(400).json({ message: "Invalid targetProviderId" });
      }
      body.targetProviderId = targetStr;
    }

    const requestedFrom = getClient(req);

    const doc = new Request({
      ...body,
      userId: req.user._id,
      status: "pending",
      requestedFrom,
      createdByRole: req.user?.role || "user",
      providerAmount: 0,
      systemFee: 0,
      totalAmount: 0,
      priceSetByProvider: false,
      priceSetAt: null,
      pricingStatus: "not_set",
      paymentStatus: "unpaid",
      paymentMethod: "",
      paymentReference: "",
      paidAt: null,
      providerCompleted: false,
      userCompleted: false,
      completedAt: null,
      statusHistory: [],
    });

    pushStatusHistory(doc, "pending", req.user._id, "Request created");
    await doc.save();

    emitRequestEvent(req, "request_created", doc);
    await broadcastRequestToProviders(req, doc);

    return res.status(201).json(doc);
  } catch (err) {
    console.error("Create request error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * GET /api/requests/provider?providerType=towing
 */
router.get("/provider", protect, async (req, res) => {
  try {
    const { providerType } = req.query;

    if (!providerType) {
      return res.status(400).json({ message: "providerType required" });
    }

    const pt = String(providerType).trim().toLowerCase();
    const providerObjectId = new mongoose.Types.ObjectId(req.user._id);

    const pendingFilter = {
      providerType: pt,
      status: "pending",
      declinedBy: { $ne: providerObjectId },
      $or: [
        { targetProviderId: { $exists: false } },
        { targetProviderId: null },
        { targetProviderId: providerObjectId },
      ],
    };

    const ongoingFilter = {
      providerType: pt,
      assignedProviderId: providerObjectId,
      status: { $in: ["assigned", "arrived", "quoted", "awaiting_payment", "in_progress", "awaiting_dual_confirmation"] },
    };

    const completedFilter = {
      providerType: pt,
      assignedProviderId: providerObjectId,
      status: { $in: ["completed", "cancelled"] },
    };

    const [pending, ongoing, completed] = await Promise.all([
      Request.find(pendingFilter).sort({ createdAt: -1 }).limit(200),
      Request.find(ongoingFilter).sort({ updatedAt: -1 }).limit(50),
      Request.find(completedFilter).sort({ updatedAt: -1 }).limit(200),
    ]);

    return res.json({
      pending,
      ongoing,
      completed,
      requests: pending,
    });
  } catch (err) {
    console.error("Get provider requests error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * GET /api/requests/my
 * logged-in user's own requests
 */
router.get("/my", protect, async (req, res) => {
  try {
    const userObjectId = new mongoose.Types.ObjectId(req.user._id);

    const requests = await Request.find({ userId: userObjectId })
      .sort({ createdAt: -1 })
      .limit(50);

    return res.json(requests);
  } catch (err) {
    console.error("Get my requests error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * POST /api/requests/:id/decline
 */
router.post("/:id/decline", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const providerId = String(req.user._id);
    const providerObjectId = new mongoose.Types.ObjectId(providerId);

    const request = await Request.findById(id);
    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (normalizeStatus(request.status) !== "pending") {
      return res.status(400).json({ message: "Only pending requests can be declined" });
    }

    const target = request.targetProviderId ? String(request.targetProviderId) : null;
    if (target && target !== providerId) {
      return res.status(403).json({ message: "Not allowed to decline this request" });
    }

    await Request.updateOne(
      { _id: id },
      { $addToSet: { declinedBy: providerObjectId } }
    );

    const updated = await Request.findById(id);
    emitRequestEvent(req, "request_updated", updated);

    return res.json({ message: "Request declined" });
  } catch (err) {
    console.error("Decline request error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * GET /api/requests/:id
 */
router.get("/:id", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const request = await Request.findById(id);
    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    return res.json(request);
  } catch (err) {
    console.error("Get request by id error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * GET /api/requests/:id/quote
 */
router.get("/:id/quote", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const request = await Request.findById(id).lean();
    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    return res.json({
      _id: request._id,
      status: request.status,
      pricingStatus: request.pricingStatus || "not_set",
      paymentStatus: request.paymentStatus || "unpaid",
      providerAmount: request.providerAmount || 0,
      systemFee: request.systemFee || 0,
      totalAmount: request.totalAmount || 0,
      priceSetByProvider: request.priceSetByProvider || false,
      priceSetAt: request.priceSetAt || null,
      paymentMethod: request.paymentMethod || "",
      paymentReference: request.paymentReference || "",
      paidAt: request.paidAt || null,
      providerCompleted: !!request.providerCompleted,
      userCompleted: !!request.userCompleted,
      completedAt: request.completedAt || null,
    });
  } catch (err) {
    console.error("Get request quote error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * POST /api/requests/:id/assign
 * provider accepts request
 */
router.post("/:id/assign", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const providerId = String(req.user._id);

    const request = await Request.findById(id);
    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (normalizeStatus(request.status) !== "pending") {
      return res.status(400).json({ message: "Request already taken" });
    }

    const target = request.targetProviderId ? String(request.targetProviderId) : null;
    if (target && target !== providerId) {
      return res.status(403).json({
        message: "This request is assigned to a different provider",
      });
    }

    const provider = await User.findById(providerId).select("-password");
    if (!provider) {
      return res.status(404).json({ message: "Provider not found" });
    }

    if (String(provider.role || "").toLowerCase() !== "provider") {
      return res.status(403).json({ message: "Only providers can accept requests" });
    }

    if (!provider.isApprovedProvider) {
      return res.status(403).json({
        message: "Your provider account is awaiting approval",
      });
    }

    if (!provider.isAvailable) {
      return res.status(403).json({
        message: "You are currently unavailable. Turn availability on first.",
      });
    }

    const providerRaw = provider.getDecrypted ? provider.getDecrypted() : provider.toObject();

    request.status = "assigned";
    request.assignedProviderId = new mongoose.Types.ObjectId(providerId);
    request.assignedProviderName = providerRaw.name || "";
    request.assignedProviderPhone = providerRaw.phone || "";
    request.assignedProviderRating = provider.rating || 0.0;

    pushStatusHistory(request, "assigned", providerId, "Request accepted by provider");
    await request.save();

    await User.findByIdAndUpdate(providerId, {
      $set: { currentRequestId: request._id },
    });

    emitRequestEvent(req, "request_updated", request);
    return res.json(request);
  } catch (err) {
    console.error("Assign request error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * PATCH /api/requests/:id/set-price
 * provider sets total amount user will pay
 */
router.patch("/:id/set-price", protect, async (req, res) => {
  try {
    const { id } = req.params;
    const { providerAmount } = req.body || {};

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    if (providerAmount === undefined || providerAmount === null) {
      return res.status(400).json({ message: "providerAmount is required" });
    }

    const amount = Number(providerAmount);

    if (!Number.isFinite(amount) || amount <= 0) {
      return res.status(400).json({
        message: "providerAmount must be a valid number greater than 0",
      });
    }

    const request = await Request.findById(id);
    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!request.assignedProviderId) {
      return res.status(400).json({ message: "No provider assigned to this request" });
    }

    if (!isAssignedProvider(request, req.user._id) && !isAdmin(req.user)) {
      return res.status(403).json({
        message: "You are not allowed to set price for this request",
      });
    }

    const currentStatus = normalizeStatus(request.status);
    if (!["assigned", "arrived"].includes(currentStatus)) {
      return res.status(400).json({
        message: `Price can only be set when request is assigned or arrived. Current status: ${request.status}`,
      });
    }

    const totalAmount = amount;
    const systemFee = calculateSystemFee(totalAmount);
    const providerNetAmount = Number((totalAmount - systemFee).toFixed(2));

    request.providerAmount = providerNetAmount;
    request.systemFee = systemFee;
    request.totalAmount = totalAmount;
    request.priceSetByProvider = true;
    request.priceSetAt = new Date();
    request.pricingStatus = "quoted";
    request.status = "awaiting_payment";
    request.paymentStatus = "unpaid";

    pushStatusHistory(
      request,
      "awaiting_payment",
      req.user._id,
      "Provider quoted service amount"
    );

    await request.save();

    emitRequestEvent(req, "request_updated", request);

    return res.json({
      message: "Price set successfully",
      request: {
        _id: request._id,
        status: request.status,
        pricingStatus: request.pricingStatus,
        paymentStatus: request.paymentStatus,
        providerAmount: request.providerAmount,
        systemFee: request.systemFee,
        totalAmount: request.totalAmount,
        priceSetAt: request.priceSetAt,
      },
    });
  } catch (err) {
    console.error("Set request price error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * PATCH /api/requests/:id/status
 * controlled status update
 */
router.patch("/:id/status", protect, async (req, res) => {
  try {
    const { id } = req.params;
    const { status, note } = req.body || {};

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    if (!status) {
      return res.status(400).json({ message: "status required" });
    }

    const nextStatus = normalizeStatus(status);

    if (!isValidStatus(nextStatus)) {
      return res.status(400).json({ message: "Invalid status value" });
    }

    const request = await Request.findById(id);
    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    const currentStatus = normalizeStatus(request.status);

    const requester = isRequestOwner(request, req.user._id);
    const assignedProvider = isAssignedProvider(request, req.user._id);
    const admin = isAdmin(req.user);

    if (!requester && !assignedProvider && !admin) {
      return res.status(403).json({ message: "Not allowed to update this request" });
    }

    if (currentStatus === nextStatus) {
      return res.json(request);
    }

    if (!admin && !canTransitionStatus(currentStatus, nextStatus)) {
      return res.status(400).json({
        message: `Invalid status transition from ${currentStatus} to ${nextStatus}`,
      });
    }

    if (!admin) {
      if (nextStatus === "arrived" || nextStatus === "in_progress") {
        if (!assignedProvider) {
          return res.status(403).json({
            message: "Only assigned provider can move request to this status",
          });
        }
      }

      if (nextStatus === "completed") {
        if (!assignedProvider) {
          return res.status(403).json({
            message: "Only assigned provider can mark request as completed for now",
          });
        }
      }

      if (nextStatus === "cancelled") {
        const allowed = requester || assignedProvider;
        if (!allowed) {
          return res.status(403).json({
            message: "You are not allowed to cancel this request",
          });
        }
      }
    }

    request.status = nextStatus;

    if (nextStatus === "completed") {
      request.completedAt = new Date();

      if (request.assignedProviderId) {
        await User.findByIdAndUpdate(request.assignedProviderId, {
          $set: { currentRequestId: null },
        });
      }
    }

    pushStatusHistory(
      request,
      nextStatus,
      req.user._id,
      typeof note === "string" ? note : ""
    );

    await request.save();

    emitRequestEvent(req, "request_updated", request);
    return res.json(request);
  } catch (err) {
    console.error("Update status error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * POST /api/requests/:id/provider-complete
 * provider marks their side complete
 */
router.post("/:id/provider-complete", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const request = await Request.findById(id);
    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!isAssignedProvider(request, req.user._id) && !isAdmin(req.user)) {
      return res.status(403).json({ message: "Only assigned provider can do this" });
    }

    request.providerCompleted = true;

    if (request.userCompleted) {
      pushStatusHistory(
        request,
        "completed",
        req.user._id,
        "Provider completed and user had already completed"
      );

      const result = await releaseEscrowPayment(request);
      emitRequestEvent(req, "request_updated", result.request);

      return res.json(result);
    }

    request.status = "awaiting_dual_confirmation";
    await request.save();
    emitRequestEvent(req, "request_updated", request);

    return res.json({
      message: "Provider marked complete. Waiting for user confirmation.",
      request,
    });
  } catch (err) {
    console.error("Provider complete error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * POST /api/requests/:id/user-confirm-complete
 * user confirms completion
 */
router.post("/:id/user-confirm-complete", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const request = await Request.findById(id);
    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!isRequestOwner(request, req.user._id) && !isAdmin(req.user)) {
      return res.status(403).json({ message: "Only request owner can do this" });
    }

    request.userCompleted = true;

    if (request.providerCompleted) {
      pushStatusHistory(
        request,
        "completed",
        req.user._id,
        "User completed and provider had already completed"
      );

      const result = await releaseEscrowPayment(request);
      emitRequestEvent(req, "request_updated", result.request);

      return res.json(result);
    }

    request.status = "awaiting_dual_confirmation";
    await request.save();
    emitRequestEvent(req, "request_updated", request);

    return res.json({
      message: "User confirmed completion. Waiting for provider confirmation.",
      request,
    });
  } catch (err) {
    console.error("User confirm completion error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * GET /api/requests/:id/location
 */
router.get("/:id/location", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const request = await Request.findById(id);
    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    const lat = request.userLocation?.lat ?? 0;
    const lng = request.userLocation?.lng ?? 0;

    return res.json({ lat, lng });
  } catch (err) {
    console.error("Get user location error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

export default router;