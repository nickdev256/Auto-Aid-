// ✅ FILE NAME: routes/payments.js
import express from "express";
import mongoose from "mongoose";
import Request from "../models/Request.js";
import User from "../models/User.js";
import { protect } from "../middleware/authMiddleware.js";

const router = express.Router();

/* =================================================
   HELPERS
================================================= */
function isValidObjectId(id) {
  return mongoose.Types.ObjectId.isValid(id);
}

function toNumber(value, fallback = 0) {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
}

function normalizeMethod(value = "") {
  return String(value || "").trim().toLowerCase();
}

function getPayableAmount(requestDoc) {
  return (
    toNumber(requestDoc?.providerAmount) ||
    toNumber(requestDoc?.agreedAmount) ||
    toNumber(requestDoc?.finalAmount) ||
    toNumber(requestDoc?.quoteAmount) ||
    toNumber(requestDoc?.quotedAmount) ||
    toNumber(requestDoc?.totalAmount) ||
    toNumber(requestDoc?.amount) ||
    toNumber(requestDoc?.price) ||
    0
  );
}

function canUserPayRequest(reqUser, requestDoc) {
  const myId = String(reqUser?._id || "");
  const userId = String(requestDoc?.userId || "");
  const role = String(reqUser?.role || "").toLowerCase();
  return role === "admin" || myId === userId;
}

function emitRequestUpdate(req, requestDoc) {
  try {
    const io = req.app.get("io");
    if (!io || !requestDoc) return;

    const requestId = String(requestDoc._id || "");
    const assignedProviderId = String(
      requestDoc.assignedProviderId || requestDoc.assignedTo || ""
    );
    const targetProviderId = String(requestDoc.targetProviderId || "");
    const userId = String(requestDoc.userId || "");
    const providerType = String(requestDoc.providerType || "").trim().toLowerCase();

    if (requestId) {
      io.to(`request:${requestId}`).emit("request_updated", requestDoc);
      io.to(`chat_${requestId}`).emit("request_updated", requestDoc);
    }

    if (userId) {
      io.to(`user:${userId}`).emit("request_updated", requestDoc);
      io.to(`user:${userId}`).emit("notify", {
        id: `payment_${requestId}_${Date.now()}`,
        type: "payment",
        title: "Payment successful",
        body: `Your payment for request #${requestId.slice(-6)} was successful.`,
        requestId,
        createdAt: new Date().toISOString(),
      });
    }

    if (assignedProviderId) {
      io.to(`provider:${assignedProviderId}`).emit("request_updated", requestDoc);
      io.to(`provider:${assignedProviderId}`).emit("notify", {
        id: `provider_payment_${requestId}_${Date.now()}`,
        type: "payment",
        title: "Payment received",
        body: `Payment for request #${requestId.slice(-6)} has been recorded.`,
        requestId,
        createdAt: new Date().toISOString(),
      });
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

/* =================================================
   1) CREATE / COMPLETE PAYMENT
   POST /api/payments
================================================= */
router.post("/", protect, async (req, res) => {
  try {
    const {
      requestId,
      amount,
      method = "cash",
      phoneNumber = "",
      txRef = "",
      transactionId = "",
      providerAmount,
      markCompleted = true,
    } = req.body || {};

    if (!requestId || !isValidObjectId(requestId)) {
      return res.status(400).json({ message: "Valid requestId is required" });
    }

    const requestDoc = await Request.findById(requestId);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!canUserPayRequest(req.user, requestDoc)) {
      return res.status(403).json({ message: "Not allowed to pay for this request" });
    }

    const normalizedMethod = normalizeMethod(method);
    const safeAmount = toNumber(amount, 0);
    const existingAmount = getPayableAmount(requestDoc);
    const finalPaidAmount = safeAmount > 0 ? safeAmount : existingAmount;

    if (finalPaidAmount <= 0) {
      return res.status(400).json({
        message: "A valid payment amount is required",
      });
    }

    if (
      String(requestDoc.paymentStatus || "").toLowerCase() === "paid"
    ) {
      return res.json({
        ok: true,
        message: "Request is already marked as paid",
        paymentStatus: "paid",
        request: requestDoc,
      });
    }

    const assignedProviderId =
      requestDoc.assignedProviderId || requestDoc.assignedTo || null;

    let resolvedProviderAmount = toNumber(providerAmount, 0);
    if (resolvedProviderAmount <= 0) {
      resolvedProviderAmount = finalPaidAmount;
    }

    requestDoc.paymentStatus = "paid";
    requestDoc.paymentMethod = normalizedMethod || "cash";
    requestDoc.paymentAmount = finalPaidAmount;
    requestDoc.providerAmount = resolvedProviderAmount;
    requestDoc.paidAt = new Date();
    requestDoc.paymentReference =
      String(transactionId || txRef || "").trim() || `PAY-${Date.now()}`;

    if (phoneNumber) {
      requestDoc.paymentPhoneNumber = String(phoneNumber).trim();
    }

    if (assignedProviderId && !requestDoc.assignedProviderId) {
      requestDoc.assignedProviderId = assignedProviderId;
    }

    if (
      markCompleted &&
      !["completed"].includes(String(requestDoc.status || "").toLowerCase())
    ) {
      requestDoc.status = "completed";
    }

    await requestDoc.save();

    emitRequestUpdate(req, requestDoc);

    return res.status(201).json({
      ok: true,
      message: "Payment recorded successfully",
      paymentStatus: requestDoc.paymentStatus,
      paymentAmount: requestDoc.paymentAmount,
      providerAmount: requestDoc.providerAmount,
      paidAt: requestDoc.paidAt,
      request: requestDoc,
    });
  } catch (err) {
    console.error("Create payment error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   2) GET PAYMENT STATUS FOR A REQUEST
   GET /api/payments/request/:requestId
================================================= */
router.get("/request/:requestId", protect, async (req, res) => {
  try {
    const { requestId } = req.params;

    if (!isValidObjectId(requestId)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const requestDoc = await Request.findById(requestId);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    const myId = String(req.user?._id || "");
    const role = String(req.user?.role || "").toLowerCase();

    const allowed =
      role === "admin" ||
      myId === String(requestDoc.userId || "") ||
      myId === String(requestDoc.assignedProviderId || requestDoc.assignedTo || "");

    if (!allowed) {
      return res.status(403).json({ message: "Access denied" });
    }

    return res.json({
      requestId: String(requestDoc._id),
      status: requestDoc.status || "",
      paymentStatus: requestDoc.paymentStatus || "unpaid",
      paymentMethod: requestDoc.paymentMethod || "",
      paymentAmount: toNumber(requestDoc.paymentAmount, 0),
      providerAmount: toNumber(requestDoc.providerAmount, 0),
      paidAt: requestDoc.paidAt || null,
      paymentReference: requestDoc.paymentReference || "",
    });
  } catch (err) {
    console.error("Get payment status error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   3) MANUAL VERIFY / RECONFIRM PAYMENT
   PATCH /api/payments/:requestId/verify
================================================= */
router.patch("/:requestId/verify", protect, async (req, res) => {
  try {
    const { requestId } = req.params;

    if (!isValidObjectId(requestId)) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const requestDoc = await Request.findById(requestId);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!canUserPayRequest(req.user, requestDoc) && String(req.user?.role || "").toLowerCase() !== "provider") {
      return res.status(403).json({ message: "Access denied" });
    }

    const finalPaidAmount =
      toNumber(req.body?.amount, 0) > 0
        ? toNumber(req.body?.amount, 0)
        : getPayableAmount(requestDoc);

    if (finalPaidAmount <= 0) {
      return res.status(400).json({ message: "No valid amount found to verify" });
    }

    requestDoc.paymentStatus = "paid";
    requestDoc.paymentAmount = finalPaidAmount;
    requestDoc.providerAmount =
      toNumber(req.body?.providerAmount, 0) > 0
        ? toNumber(req.body?.providerAmount, 0)
        : toNumber(requestDoc.providerAmount, 0) > 0
        ? toNumber(requestDoc.providerAmount, 0)
        : finalPaidAmount;

    requestDoc.paymentMethod =
      normalizeMethod(req.body?.method || requestDoc.paymentMethod || "cash");
    requestDoc.paidAt = requestDoc.paidAt || new Date();
    requestDoc.paymentReference =
      String(req.body?.transactionId || req.body?.txRef || requestDoc.paymentReference || "").trim() ||
      `PAY-${Date.now()}`;

    if (String(req.body?.markCompleted || "true").toLowerCase() !== "false") {
      requestDoc.status = "completed";
    }

    await requestDoc.save();

    emitRequestUpdate(req, requestDoc);

    return res.json({
      ok: true,
      message: "Payment verified successfully",
      paymentStatus: requestDoc.paymentStatus,
      paymentAmount: requestDoc.paymentAmount,
      providerAmount: requestDoc.providerAmount,
      request: requestDoc,
    });
  } catch (err) {
    console.error("Verify payment error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   4) OPTIONAL SANDBOX INITIATE
   POST /api/payments/initiate
   Returns a fake success response for local testing
================================================= */
router.post("/initiate", protect, async (req, res) => {
  try {
    const {
      requestId,
      amount,
      method = "mobile_money",
      phoneNumber = "",
    } = req.body || {};

    if (!requestId || !isValidObjectId(requestId)) {
      return res.status(400).json({ message: "Valid requestId is required" });
    }

    const requestDoc = await Request.findById(requestId);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!canUserPayRequest(req.user, requestDoc)) {
      return res.status(403).json({ message: "Not allowed" });
    }

    const payableAmount = toNumber(amount, 0) > 0 ? toNumber(amount, 0) : getPayableAmount(requestDoc);

    return res.json({
      ok: true,
      message: "Sandbox payment initiated",
      status: "pending",
      requestId,
      amount: payableAmount,
      method: normalizeMethod(method),
      phoneNumber: String(phoneNumber || "").trim(),
      txRef: `TX-${Date.now()}`,
    });
  } catch (err) {
    console.error("Initiate payment error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

export default router;