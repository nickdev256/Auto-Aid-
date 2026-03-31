import express from "express";
import mongoose from "mongoose";
import Request from "../models/Request.js";
import { protect } from "../middleware/authMiddleware.js";

const router = express.Router();

function isValidObjectId(id) {
  return mongoose.Types.ObjectId.isValid(id);
}

function normalizeStatus(value = "") {
  return String(value || "").trim().toLowerCase();
}

function normalizeProviderType(value = "") {
  const v = String(value || "").trim().toLowerCase();

  if (["garage", "mechanic", "repair"].includes(v)) return "garage";
  if (["fuel", "fuel delivery", "petrol", "diesel"].includes(v)) return "fuel";
  if (["towing", "tow", "towing track", "towing truck"].includes(v)) return "towing";
  if (["ambulance", "medical", "emergency"].includes(v)) return "ambulance";

  return v;
}

function isSupportedProviderType(value = "") {
  return ["garage", "fuel", "towing", "ambulance"].includes(
    normalizeProviderType(value)
  );
}

function isAdmin(user) {
  return normalizeStatus(user?.role) === "admin";
}

function isProvider(user) {
  return normalizeStatus(user?.role) === "provider";
}

function isUser(user) {
  return normalizeStatus(user?.role) === "user";
}

function getCurrentProviderType(user) {
  return normalizeProviderType(
    user?.businessType ||
      user?.providerType ||
      user?.serviceType ||
      ""
  );
}

function isProviderForRequest(user, requestDoc) {
  const myId = String(user?._id || "");
  const assignedProviderId = String(
    requestDoc?.assignedProviderId || requestDoc?.assignedTo || ""
  );
  const targetProviderId = String(requestDoc?.targetProviderId || "");

  return (
    (!!assignedProviderId && myId === assignedProviderId) ||
    (!!targetProviderId && myId === targetProviderId)
  );
}

function isUserForRequest(user, requestDoc) {
  const myId = String(user?._id || "");
  const userId = String(requestDoc?.userId || "");
  return !!myId && !!userId && myId === userId;
}

function canProviderManageRequest(user, requestDoc) {
  return isAdmin(user) || (isProvider(user) && isProviderForRequest(user, requestDoc));
}

function canUserManageRequest(user, requestDoc) {
  return isAdmin(user) || isUserForRequest(user, requestDoc);
}

function getActorType(user, requestDoc) {
  if (isAdmin(user)) return "admin";
  if (isProvider(user) && isProviderForRequest(user, requestDoc)) return "provider";
  if (isUserForRequest(user, requestDoc)) return "user";
  return "unknown";
}

function isTerminalStatus(status) {
  const s = normalizeStatus(status);
  return s === "completed" || s === "cancelled";
}

function emitRequestUpdate(req, requestDoc) {
  try {
    const io = req.app.get("io");
    if (!io || !requestDoc) return;

    const requestId = String(requestDoc._id || "");
    const assignedProviderId = String(requestDoc.assignedProviderId || requestDoc.assignedTo || "");
    const targetProviderId = String(requestDoc.targetProviderId || "");
    const userId = String(requestDoc.userId || "");
    const providerType = normalizeProviderType(requestDoc.providerType || requestDoc.service);

    if (requestId) {
      io.to(`request:${requestId}`).emit("request_updated", requestDoc);
      io.to(`chat_${requestId}`).emit("request_updated", requestDoc);
    }

    if (userId) {
      io.to(`user:${userId}`).emit("request_updated", requestDoc);
    }

    if (assignedProviderId) {
      io.to(`provider:${assignedProviderId}`).emit("request_updated", requestDoc);
    }

    if (targetProviderId) {
      io.to(`provider:${targetProviderId}`).emit("request_updated", requestDoc);
    }

    if (providerType) {
      io.to(`type:${providerType}`).emit("request_updated", requestDoc);
    }
  } catch (err) {
    console.error("emitRequestUpdate error:", err.message);
  }
}

function emitNotification(req, room, payload) {
  try {
    const io = req.app.get("io");
    if (!io || !room) return;
    io.to(room).emit("notify", payload);
  } catch (err) {
    console.error("emitNotification error:", err.message);
  }
}

function applyStatusTimestamps(requestDoc, nextStatus) {
  const status = normalizeStatus(nextStatus);
  const now = new Date();

  if (status === "accepted" && !requestDoc.assignedAt) requestDoc.assignedAt = now;
  if (status === "started" && !requestDoc.tripStartedAt) requestDoc.tripStartedAt = now;
  if (status === "arrived" && !requestDoc.arrivedAt) requestDoc.arrivedAt = now;
  if (status === "quotation_sent" && !requestDoc.quoteSentAt) requestDoc.quoteSentAt = now;
  if (status === "paid" && !requestDoc.paymentReadyAt) requestDoc.paymentReadyAt = now;
  if (status === "provider_done" && !requestDoc.providerCompletedAt) requestDoc.providerCompletedAt = now;
  if (status === "completed" && !requestDoc.completedAt) requestDoc.completedAt = now;
  if (status === "cancelled" && !requestDoc.cancelledAt) requestDoc.cancelledAt = now;
}

function validateStatusTransition(requestDoc, nextStatus, actor) {
  const currentStatus = normalizeStatus(requestDoc?.status || "pending");
  const targetStatus = normalizeStatus(nextStatus);

  if (!targetStatus) {
    return { ok: false, message: "Target status is required" };
  }

  if (currentStatus === targetStatus) {
    return { ok: true };
  }

  if (isTerminalStatus(currentStatus)) {
    return { ok: false, message: `Request is already ${currentStatus} and cannot change` };
  }

  const paymentStatus = normalizeStatus(requestDoc?.paymentStatus || "unpaid");
  const paymentConfirmedByProvider = !!requestDoc?.paymentConfirmedByProvider;
  const providerCompleted = !!requestDoc?.providerCompleted;
  const userCompleted = !!requestDoc?.userCompleted;

  switch (targetStatus) {
    case "accepted":
      if (currentStatus !== "pending") {
        return { ok: false, message: "Only pending requests can be accepted" };
      }
      if (!["provider", "admin"].includes(actor)) {
        return { ok: false, message: "Only provider or admin can accept request" };
      }
      return { ok: true };

    case "started":
      if (currentStatus !== "accepted") {
        return { ok: false, message: "Request must be accepted before starting" };
      }
      if (!["provider", "admin"].includes(actor)) {
        return { ok: false, message: "Only provider or admin can start job" };
      }
      return { ok: true };

    case "arrived":
      if (currentStatus !== "started") {
        return { ok: false, message: "Request must be started before arriving" };
      }
      if (!["provider", "admin"].includes(actor)) {
        return { ok: false, message: "Only provider or admin can mark arrived" };
      }
      return { ok: true };

    case "quotation_sent":
      if (currentStatus !== "arrived") {
        return { ok: false, message: "Provider must arrive before sending quotation" };
      }
      if (!["provider", "admin"].includes(actor)) {
        return { ok: false, message: "Only provider or admin can send quotation" };
      }
      return { ok: true };

    case "paid":
      if (currentStatus !== "quotation_sent") {
        return { ok: false, message: "Quotation must be sent before payment" };
      }
      return { ok: true };

    case "provider_done":
      if (currentStatus !== "paid") {
        return { ok: false, message: "Payment must be completed before provider marks job done" };
      }
      if (!(paymentStatus === "paid" || paymentConfirmedByProvider)) {
        return { ok: false, message: "Payment must be confirmed before provider marks job done" };
      }
      if (!["provider", "admin"].includes(actor)) {
        return { ok: false, message: "Only provider or admin can mark job done" };
      }
      return { ok: true };

    case "completed":
      if (currentStatus !== "provider_done") {
        return { ok: false, message: "Provider must mark job done first" };
      }
      if (!providerCompleted) {
        return { ok: false, message: "Provider must mark job done first" };
      }
      if (!userCompleted) {
        return { ok: false, message: "User must confirm completion" };
      }
      return { ok: true };

    case "cancelled":
      return { ok: true };

    default:
      return { ok: false, message: `Unsupported target status: ${targetStatus}` };
  }
}

/* =================================================
   POST /api/requests
================================================= */
router.post("/", protect, async (req, res) => {
  try {
    const {
      providerType,
      service,
      targetProviderId,
      vehicleInfo = "",
      problem = "",
      towType = "",
      note = "",
      urgency = "normal",
      userLocation = null,
      amount = 0,
      price = 0,
    } = req.body || {};

    const userId = req.user?._id;

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const normalizedType = normalizeProviderType(providerType || service);

    if (!normalizedType) {
      return res.status(400).json({ message: "providerType or service is required" });
    }

    if (!isSupportedProviderType(normalizedType)) {
      return res.status(400).json({ message: "Invalid service type" });
    }

    const requestDoc = await Request.create({
      userId,
      userName: req.user?.name || req.user?.fullName || "",
      userPhone: req.user?.phone || "",
      providerType: normalizedType,
      service: normalizedType,
      targetProviderId: targetProviderId || null,
      vehicleInfo,
      problem,
      towType,
      note,
      urgency,
      userLocation,
      amount,
      price,
      status: "pending",
      paymentStatus: "unpaid",
      paymentConfirmedByProvider: false,
      providerCompleted: false,
      userCompleted: false,
      quotationAccepted: false,
      priceSetByProvider: false,
    });

    emitRequestUpdate(req, requestDoc);

    if (normalizedType) {
      emitNotification(req, `type:${normalizedType}`, {
        id: `new_request_${requestDoc._id}_${Date.now()}`,
        type: "request",
        title: "New request",
        body: `A new ${normalizedType} request is available.`,
        requestId: String(requestDoc._id),
        createdAt: new Date().toISOString(),
      });
    }

    return res.status(201).json(requestDoc);
  } catch (err) {
    console.error("Create request error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   GET /api/requests/my
================================================= */
router.get("/my", protect, async (req, res) => {
  try {
    const userId = req.user?._id;
    const requests = await Request.find({ userId }).sort({ createdAt: -1 });
    return res.json(requests);
  } catch (err) {
    console.error("Get my requests error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   GET /api/requests/provider
================================================= */
router.get("/provider", protect, async (req, res) => {
  try {
    const providerId = String(req.user?._id || "");
    const providerType = getCurrentProviderType(req.user);

    if (!providerId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!providerType || !isSupportedProviderType(providerType)) {
      return res.status(400).json({ message: "Provider type is missing or invalid" });
    }

    const query = {
      $or: [
        { assignedProviderId: providerId },
        { targetProviderId: providerId },
        {
          $and: [
            { assignedProviderId: null },
            { targetProviderId: null },
            { providerType: providerType },
            { status: "pending" },
          ],
        },
      ],
    };

    const all = await Request.find(query).sort({ createdAt: -1 });

    const buckets = {
      pending: [],
      ongoing: [],
      completed: [],
    };

    all.forEach((r) => {
      const s = normalizeStatus(r.status);
      if (s === "pending") buckets.pending.push(r);
      else if (["completed", "cancelled"].includes(s)) buckets.completed.push(r);
      else buckets.ongoing.push(r);
    });

    return res.json(buckets);
  } catch (err) {
    console.error("Get provider buckets error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   POST /api/requests/:id/assign
================================================= */
router.post("/:id/assign", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!isValidObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const requestDoc = await Request.findById(id);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!isProvider(req.user) && !isAdmin(req.user)) {
      return res.status(403).json({ message: "Only provider can accept requests" });
    }

    const actor = isAdmin(req.user) ? "admin" : "provider";
    const validation = validateStatusTransition(requestDoc, "accepted", actor);
    if (!validation.ok) {
      return res.status(400).json({ message: validation.message });
    }

    const requestType = normalizeProviderType(
      requestDoc.providerType || requestDoc.service
    );
    const myProviderType = getCurrentProviderType(req.user);

    if (!isAdmin(req.user)) {
      if (!myProviderType || !isSupportedProviderType(myProviderType)) {
        return res.status(400).json({ message: "Provider account has no valid service type" });
      }

      if (requestType !== myProviderType) {
        return res.status(403).json({
          message: `This request is for ${requestType}, but your provider type is ${myProviderType}`,
        });
      }

      if (
        requestDoc.targetProviderId &&
        String(requestDoc.targetProviderId) !== String(req.user?._id)
      ) {
        return res.status(403).json({
          message: "This request is targeted to another provider",
        });
      }
    }

    requestDoc.assignedProviderId = req.user?._id;
    requestDoc.assignedTo = req.user?._id;
    requestDoc.assignedProviderName =
      req.user?.businessName || req.user?.name || req.user?.fullName || "Provider";
    requestDoc.assignedProviderPhone = req.user?.phone || "";
    requestDoc.status = "accepted";
    applyStatusTimestamps(requestDoc, "accepted");

    await requestDoc.save();
    emitRequestUpdate(req, requestDoc);

    const userId = String(requestDoc.userId || "");
    if (userId) {
      emitNotification(req, `user:${userId}`, {
        id: `request_accepted_${requestDoc._id}_${Date.now()}`,
        type: "request",
        title: "Provider accepted request",
        body: "A provider accepted your request.",
        requestId: String(requestDoc._id),
        createdAt: new Date().toISOString(),
      });
    }

    return res.json(requestDoc);
  } catch (err) {
    console.error("Assign request error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   PATCH /api/requests/:id/status
================================================= */
router.patch("/:id/status", protect, async (req, res) => {
  try {
    const { id } = req.params;
    const status = normalizeStatus(req.body?.status);

    if (!isValidObjectId(id)) {
      return res.status(400).json({ ok: false, message: "Invalid request id" });
    }

    if (!status) {
      return res.status(400).json({ ok: false, message: "Status is required" });
    }

    const requestDoc = await Request.findById(id);
    if (!requestDoc) {
      return res.status(404).json({ ok: false, message: "Request not found" });
    }

    const actor = getActorType(req.user, requestDoc);
    if (actor === "unknown") {
      return res.status(403).json({ ok: false, message: "Access denied" });
    }

    const validation = validateStatusTransition(requestDoc, status, actor);
    if (!validation.ok) {
      return res.status(400).json({ ok: false, message: validation.message });
    }

    requestDoc.status = status;
    applyStatusTimestamps(requestDoc, status);

    await requestDoc.save();
    emitRequestUpdate(req, requestDoc);

    const requestId = String(requestDoc._id || "");
    const userId = String(requestDoc.userId || "");
    const providerId = String(requestDoc.assignedProviderId || requestDoc.assignedTo || "");

    if (status === "started" && userId) {
      emitNotification(req, `user:${userId}`, {
        id: `job_started_${requestId}_${Date.now()}`,
        type: "request",
        title: "Provider started job",
        body: "Your provider has started moving to your location.",
        requestId,
        createdAt: new Date().toISOString(),
      });
    }

    if (status === "arrived" && userId) {
      emitNotification(req, `user:${userId}`, {
        id: `provider_arrived_${requestId}_${Date.now()}`,
        type: "request",
        title: "Provider arrived",
        body: "Your provider has arrived.",
        requestId,
        createdAt: new Date().toISOString(),
      });
    }

    if (status === "completed" && providerId) {
      emitNotification(req, `provider:${providerId}`, {
        id: `job_completed_${requestId}_${Date.now()}`,
        type: "request",
        title: "Job completed",
        body: "The user confirmed the job is complete.",
        requestId,
        createdAt: new Date().toISOString(),
      });
    }

    return res.json({
      ok: true,
      message: "Status updated successfully",
      request: requestDoc,
    });
  } catch (err) {
    console.error("STATUS UPDATE ERROR:", err);
    return res.status(500).json({
      ok: false,
      message: "Server error",
      error: err.message,
    });
  }
});

/* =================================================
   PATCH /api/requests/:id/set-price
================================================= */
router.patch("/:id/set-price", protect, async (req, res) => {
  try {
    const { id } = req.params;
    const { providerAmount } = req.body || {};

    if (!isValidObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const requestDoc = await Request.findById(id);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!canProviderManageRequest(req.user, requestDoc)) {
      return res.status(403).json({ message: "Only provider can set price" });
    }

    const amount = Number(providerAmount || 0);
    if (!Number.isFinite(amount) || amount <= 0) {
      return res.status(400).json({ message: "Valid providerAmount is required" });
    }

    if (normalizeStatus(requestDoc.status) !== "arrived") {
      return res.status(400).json({ message: "Provider must arrive before sending quotation" });
    }

    const systemFee = Math.max(3000, Math.round(amount * 0.1));
    const totalAmount = amount;

    requestDoc.providerAmount = amount - systemFee > 0 ? amount - systemFee : amount;
    requestDoc.systemFee = systemFee;
    requestDoc.totalAmount = totalAmount;
    requestDoc.quoteAmount = totalAmount;
    requestDoc.quotedAmount = requestDoc.providerAmount;
    requestDoc.priceSetByProvider = true;
    requestDoc.priceSetAt = new Date();
    requestDoc.pricingStatus = "quoted";
    requestDoc.quotationAccepted = false;
    requestDoc.status = "quotation_sent";
    applyStatusTimestamps(requestDoc, "quotation_sent");

    await requestDoc.save();
    emitRequestUpdate(req, requestDoc);

    const userId = String(requestDoc.userId || "");
    if (userId) {
      emitNotification(req, `user:${userId}`, {
        id: `quote_ready_${requestDoc._id}_${Date.now()}`,
        type: "payment",
        title: "Quotation ready",
        body: "Your provider has sent a quotation. You can now view and pay.",
        requestId: String(requestDoc._id),
        createdAt: new Date().toISOString(),
      });
    }

    return res.json({
      message: "Quotation sent successfully",
      request: {
        providerAmount: requestDoc.providerAmount,
        systemFee: requestDoc.systemFee,
        totalAmount: requestDoc.totalAmount,
        pricingStatus: requestDoc.pricingStatus,
        priceSetByProvider: true,
        quotationAccepted: requestDoc.quotationAccepted,
        status: requestDoc.status,
      },
    });
  } catch (err) {
    console.error("Set request price error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   PATCH /api/requests/:id/accept-quotation
================================================= */
router.patch("/:id/accept-quotation", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!isValidObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const requestDoc = await Request.findById(id);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!canUserManageRequest(req.user, requestDoc)) {
      return res.status(403).json({ message: "Only user can accept quotation" });
    }

    if (normalizeStatus(requestDoc.status) !== "quotation_sent") {
      return res.status(400).json({ message: "Quotation is not available for acceptance" });
    }

    if (requestDoc.quotationAccepted) {
      return res.status(400).json({
        ok: false,
        message: "Quotation already accepted",
      });
    }

    requestDoc.quotationAccepted = true;
    requestDoc.pricingStatus = "accepted";

    await requestDoc.save();
    emitRequestUpdate(req, requestDoc);

    const providerId = String(requestDoc.assignedProviderId || requestDoc.assignedTo || "");
    if (providerId) {
      emitNotification(req, `provider:${providerId}`, {
        id: `quotation_accepted_${requestDoc._id}_${Date.now()}`,
        type: "payment",
        title: "Quotation accepted",
        body: "The customer accepted the quotation and can proceed to payment.",
        requestId: String(requestDoc._id),
        createdAt: new Date().toISOString(),
      });
    }

    return res.json({
      ok: true,
      message: "Quotation accepted successfully",
      request: requestDoc,
    });
  } catch (err) {
    console.error("Accept quotation error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   GET /api/requests/:id/quote
================================================= */
router.get("/:id/quote", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!isValidObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const requestDoc = await Request.findById(id);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    const actor = getActorType(req.user, requestDoc);
    if (actor === "unknown") {
      return res.status(403).json({ message: "Access denied" });
    }

    return res.json({
      providerAmount: Number(requestDoc.providerAmount || 0),
      systemFee: Number(requestDoc.systemFee || 0),
      totalAmount:
        Number(requestDoc.totalAmount || 0) ||
        Number(requestDoc.quoteAmount || 0) ||
        Number(requestDoc.amount || 0) ||
        Number(requestDoc.price || 0),
      pricingStatus: requestDoc.pricingStatus || "not_set",
      paymentStatus: requestDoc.paymentStatus || "unpaid",
      paymentConfirmedByProvider: !!requestDoc.paymentConfirmedByProvider,
      priceSetByProvider: !!requestDoc.priceSetByProvider,
      quotationAccepted: !!requestDoc.quotationAccepted,
      status: requestDoc.status || "pending",
    });
  } catch (err) {
    console.error("Get request quote error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   GET /api/requests/:id/location
================================================= */
router.get("/:id/location", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!isValidObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const requestDoc = await Request.findById(id);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    const actor = getActorType(req.user, requestDoc);
    if (actor === "unknown") {
      return res.status(403).json({ message: "Access denied" });
    }

    return res.json({
      lat: requestDoc.userLocation?.lat || 0,
      lng: requestDoc.userLocation?.lng || 0,
    });
  } catch (err) {
    console.error("Get user location error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   POST /api/requests/:id/provider-complete
================================================= */
router.post("/:id/provider-complete", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!isValidObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const requestDoc = await Request.findById(id);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!canProviderManageRequest(req.user, requestDoc)) {
      return res.status(403).json({ message: "Only provider can mark job done" });
    }

    if (normalizeStatus(requestDoc.status) !== "paid") {
      return res.status(400).json({ message: "Request must be paid before marking job done" });
    }

    if (
      normalizeStatus(requestDoc.paymentStatus) !== "paid" &&
      !requestDoc.paymentConfirmedByProvider
    ) {
      return res.status(400).json({ message: "Payment must be confirmed before marking job done" });
    }

    requestDoc.providerCompleted = true;
    requestDoc.providerCompletedAt = new Date();
    requestDoc.status = "provider_done";
    applyStatusTimestamps(requestDoc, "provider_done");

    await requestDoc.save();
    emitRequestUpdate(req, requestDoc);

    const userId = String(requestDoc.userId || "");
    if (userId) {
      emitNotification(req, `user:${userId}`, {
        id: `provider_done_${requestDoc._id}_${Date.now()}`,
        type: "request",
        title: "Provider marked job done",
        body: "Please confirm if your job is complete.",
        requestId: String(requestDoc._id),
        createdAt: new Date().toISOString(),
      });
    }

    return res.json({
      message: "Job marked done successfully",
      request: requestDoc,
    });
  } catch (err) {
    console.error("Provider complete error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   POST /api/requests/:id/user-confirm-complete
================================================= */
router.post("/:id/user-confirm-complete", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!isValidObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const requestDoc = await Request.findById(id);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!canUserManageRequest(req.user, requestDoc)) {
      return res.status(403).json({ message: "Only user can confirm completion" });
    }

    if (normalizeStatus(requestDoc.status) !== "provider_done") {
      return res.status(400).json({ message: "Request is not awaiting your confirmation" });
    }

    if (!requestDoc.providerCompleted) {
      return res.status(400).json({ message: "Provider has not marked the job done yet" });
    }

    requestDoc.userCompleted = true;
    requestDoc.userCompletedAt = new Date();
    requestDoc.status = "completed";
    applyStatusTimestamps(requestDoc, "completed");

    await requestDoc.save();
    emitRequestUpdate(req, requestDoc);

    const providerId = String(requestDoc.assignedProviderId || requestDoc.assignedTo || "");
    if (providerId) {
      emitNotification(req, `provider:${providerId}`, {
        id: `request_completed_${requestDoc._id}_${Date.now()}`,
        type: "request",
        title: "Request completed",
        body: "The user confirmed the job is complete.",
        requestId: String(requestDoc._id),
        createdAt: new Date().toISOString(),
      });
    }

    return res.json({
      message: "Job completion confirmed successfully",
      request: requestDoc,
    });
  } catch (err) {
    console.error("User confirm complete error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   GET /api/requests/:id
================================================= */
router.get("/:id", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!isValidObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const requestDoc = await Request.findById(id);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    const actor = getActorType(req.user, requestDoc);
    if (actor === "unknown") {
      return res.status(403).json({ message: "Access denied" });
    }

    return res.json(requestDoc);
  } catch (err) {
    console.error("Get request by id error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   GET /api/requests/:id/full
================================================= */
router.get("/:id/full", protect, async (req, res) => {
  try {
    const { id } = req.params;

    if (!isValidObjectId(id)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const requestDoc = await Request.findById(id);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    const actor = getActorType(req.user, requestDoc);
    if (actor === "unknown") {
      return res.status(403).json({ message: "Access denied" });
    }

    return res.json(requestDoc);
  } catch (err) {
    console.error("Get full request error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

export default router;