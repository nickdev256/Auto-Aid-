import express from "express";
import mongoose from "mongoose";
import Request from "../models/Request.js";
import { protect } from "../middleware/authMiddleware.js";

const router = express.Router();

function isValidObjectId(id) {
  return mongoose.Types.ObjectId.isValid(id);
}

function toNumber(value, fallback = 0) {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
}

function normalizeStatus(value = "") {
  return String(value || "").trim().toLowerCase();
}

function canUserAccessRequest(reqUser, requestDoc) {
  const role = String(reqUser?.role || "").toLowerCase();
  const myId = String(reqUser?._id || "");
  const ownerId = String(requestDoc?.userId || "");
  return role === "admin" || myId === ownerId;
}

function emitRequestUpdate(req, requestDoc) {
  try {
    const io = req.app.get("io");
    if (!io || !requestDoc) return;

    const requestId = String(requestDoc._id || "");
    const assignedProviderId = String(requestDoc.assignedProviderId || requestDoc.assignedTo || "");
    const targetProviderId = String(requestDoc.targetProviderId || "");
    const userId = String(requestDoc.userId || "");
    const providerType = String(requestDoc.providerType || requestDoc.service || "").trim().toLowerCase();

    const payload = {
      _id: requestDoc._id,
      id: requestDoc._id,
      requestId: requestDoc._id,
      status: requestDoc.status,
      paymentStatus: requestDoc.paymentStatus,
      paymentMethod: requestDoc.paymentMethod,
      paymentAmount: requestDoc.paymentAmount,
      paymentPhoneNumber: requestDoc.paymentPhoneNumber,
      paymentConfirmedByProvider: !!requestDoc.paymentConfirmedByProvider,
      quotationAccepted: !!requestDoc.quotationAccepted,
      paidAt: requestDoc.paidAt,
      providerCompleted: requestDoc.providerCompleted,
      providerCompletedAt: requestDoc.providerCompletedAt,
      userCompleted: requestDoc.userCompleted,
      userCompletedAt: requestDoc.userCompletedAt,
      completedAt: requestDoc.completedAt,
      assignedProviderId: requestDoc.assignedProviderId || requestDoc.assignedTo || null,
      userId: requestDoc.userId || null,
      providerType: requestDoc.providerType || requestDoc.service || "",
      totalAmount: requestDoc.totalAmount || requestDoc.quoteAmount || requestDoc.amount || 0,
    };

    if (requestId) {
      io.to(`request:${requestId}`).emit("request_updated", payload);
      io.to(`chat_${requestId}`).emit("request_updated", payload);
    }

    if (userId) {
      io.to(`user:${userId}`).emit("request_updated", payload);
      io.to(`user:${userId}`).emit("notify", {
        id: `payment_${requestId}_${Date.now()}`,
        type: "payment",
        title: "Payment successful",
        body: `Payment for request #${requestId.slice(-6)} was successful.`,
        requestId,
        createdAt: new Date().toISOString(),
      });
    }

    if (assignedProviderId) {
      io.to(`provider:${assignedProviderId}`).emit("request_updated", payload);
      io.to(`provider:${assignedProviderId}`).emit("notify", {
        id: `provider_payment_${requestId}_${Date.now()}`,
        type: "payment",
        title: "Customer paid",
        body: `Customer payment for request #${requestId.slice(-6)} is complete.`,
        requestId,
        createdAt: new Date().toISOString(),
      });
    }

    if (targetProviderId) {
      io.to(`provider:${targetProviderId}`).emit("request_updated", payload);
    }

    if (providerType) {
      io.to(`type:${providerType}`).emit("request_updated", payload);
    }
  } catch (err) {
    console.error("emitRequestUpdate error:", err.message);
  }
}

router.post("/", protect, async (req, res) => {
  try {
    const { requestId, amount, method = "mobile_money", phoneNumber = "" } = req.body || {};

    if (!requestId || !isValidObjectId(requestId)) {
      return res.status(400).json({ ok: false, message: "Valid requestId is required" });
    }

    const requestDoc = await Request.findById(requestId);
    if (!requestDoc) {
      return res.status(404).json({ ok: false, message: "Request not found" });
    }

    if (!canUserAccessRequest(req.user, requestDoc)) {
      return res.status(403).json({ ok: false, message: "Not allowed to pay for this request" });
    }

    if (normalizeStatus(requestDoc.status) !== "quotation_sent") {
      return res.status(400).json({
        ok: false,
        message: "Payment is only allowed after quotation has been sent",
      });
    }

    if (!requestDoc.quotationAccepted) {
      return res.status(400).json({
        ok: false,
        message: "Quotation must be accepted before payment",
      });
    }

    if (!phoneNumber || !String(phoneNumber).trim()) {
      return res.status(400).json({ ok: false, message: "Phone number is required" });
    }

    const quotationAmount =
      toNumber(requestDoc.totalAmount) ||
      toNumber(requestDoc.quoteAmount) ||
      toNumber(requestDoc.amount) ||
      toNumber(amount);

    if (quotationAmount <= 0) {
      return res.status(400).json({ ok: false, message: "Quotation amount is missing" });
    }

    requestDoc.paymentMethod = String(method || "mobile_money").trim();
    requestDoc.paymentPhoneNumber = String(phoneNumber).trim();
    requestDoc.paymentAmount = quotationAmount;
    requestDoc.paymentStatus = "paid";
    requestDoc.paymentConfirmedByProvider = true;
    requestDoc.paymentConfirmedAt = new Date();
    requestDoc.paidAt = new Date();
    requestDoc.status = "paid";
    requestDoc.paymentReference = `PAY-${String(requestDoc._id).slice(-6)}-${Date.now()}`;

    if (!requestDoc.paymentReadyAt) {
      requestDoc.paymentReadyAt = new Date();
    }

    await requestDoc.save();
    emitRequestUpdate(req, requestDoc);

    return res.status(201).json({
      ok: true,
      message: "Payment successful",
      requestId: String(requestDoc._id),
      amount: quotationAmount,
      method: requestDoc.paymentMethod,
      phoneNumber: requestDoc.paymentPhoneNumber,
      paymentStatus: requestDoc.paymentStatus,
      paymentConfirmedByProvider: requestDoc.paymentConfirmedByProvider,
      status: requestDoc.status,
      reference: requestDoc.paymentReference,
    });
  } catch (err) {
    console.error("Create payment error:", err);
    return res.status(500).json({
      ok: false,
      message: "Server error",
    });
  }
});

router.get("/history", protect, async (req, res) => {
  try {
    const role = String(req.user?.role || "").toLowerCase();
    const userId = String(req.user?._id || "");

    let query = {
      paymentAmount: { $gt: 0 },
    };

    if (role === "provider") {
      query.$or = [
        { assignedProviderId: req.user._id },
        { assignedTo: req.user._id },
        { targetProviderId: req.user._id },
      ];
    } else if (role !== "admin") {
      query.userId = userId;
    }

    const requests = await Request.find(query)
      .select("_id assignedProviderName service providerType paymentAmount paymentMethod paymentStatus paymentPhoneNumber paymentConfirmedByProvider paidAt createdAt status")
      .sort({ paidAt: -1, createdAt: -1 })
      .lean();

    const payments = requests.map((r) => ({
      _id: r._id,
      id: r._id,
      requestId: r._id,
      amount: toNumber(r.paymentAmount, 0),
      method: r.paymentMethod || "mobile_money",
      paymentStatus: r.paymentStatus || "unpaid",
      reference: `PAY-${String(r._id).slice(-6)}`,
      createdAt: r.paidAt || r.createdAt || null,
      serviceName: r.service || r.providerType || "Service",
      providerName: r.assignedProviderName || "Provider",
      paymentConfirmedByProvider: !!r.paymentConfirmedByProvider,
    }));

    return res.json(payments);
  } catch (err) {
    console.error("Payment history error:", err);
    return res.status(500).json([]);
  }
});

export default router;