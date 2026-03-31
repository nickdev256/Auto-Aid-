import express from "express";
import mongoose from "mongoose";
import ExtraCharge from "../models/ExtraCharge.js";
import Request from "../models/Request.js";
import UserWallet from "../models/UserWallet.js";
import ProviderWallet from "../models/ProviderWallet.js";
import WalletTransaction from "../models/WalletTransaction.js";
import { protect } from "../middleware/authMiddleware.js";

const router = express.Router();

function isValidObjectId(id) {
  return mongoose.Types.ObjectId.isValid(id);
}

function normalizeMethod(value = "") {
  const v = String(value || "").trim().toLowerCase();
  if (["airtel_money", "wallet", "cash"].includes(v)) return v;
  return "";
}

async function getOrCreateUserWallet(userId) {
  let wallet = await UserWallet.findOne({ userId });
  if (!wallet) {
    wallet = await UserWallet.create({
      userId,
      balance: 0,
      totalTopUps: 0,
      totalSpent: 0,
      totalRefunded: 0,
    });
  }
  return wallet;
}

async function getOrCreateProviderWallet(providerId) {
  let wallet = await ProviderWallet.findOne({ providerId });
  if (!wallet) {
    wallet = await ProviderWallet.create({
      providerId,
      availableBalance: 0,
      pendingBalance: 0,
      totalEarned: 0,
      totalWithdrawn: 0,
    });
  }
  return wallet;
}

function emitExtraChargeUpdate(app, requestDoc, extraCharge) {
  try {
    const io = app.get("io");
    if (!io || !requestDoc) return;

    const requestId = String(requestDoc._id || "");
    const providerId = String(requestDoc.assignedProviderId || requestDoc.assignedTo || "");
    const userId = String(requestDoc.userId || "");

    if (requestId) {
      io.to(`request:${requestId}`).emit("extra_charge_updated", extraCharge);
    }
    if (userId) {
      io.to(`user:${userId}`).emit("extra_charge_updated", extraCharge);
    }
    if (providerId) {
      io.to(`provider:${providerId}`).emit("extra_charge_updated", extraCharge);
    }
  } catch (err) {
    console.error("emitExtraChargeUpdate error:", err.message);
  }
}

/* =================================================
   GET EXTRA CHARGES FOR REQUEST
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

    const me = String(req.user?._id || "");
    const role = String(req.user?.role || "").toLowerCase();

    const allowed =
      role === "admin" ||
      me === String(requestDoc.userId || "") ||
      me === String(requestDoc.assignedProviderId || requestDoc.assignedTo || "");

    if (!allowed) {
      return res.status(403).json({ message: "Access denied" });
    }

    const list = await ExtraCharge.find({ requestId }).sort({ createdAt: -1 });
    return res.json(list);
  } catch (err) {
    console.error("Get extra charges error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   CREATE EXTRA CHARGE (PROVIDER)
================================================= */
router.post("/", protect, async (req, res) => {
  try {
    const role = String(req.user?.role || "").toLowerCase();
    if (role !== "provider" && role !== "admin") {
      return res.status(403).json({ message: "Only providers can create extra charges" });
    }

    const {
      requestId,
      amount,
      reason,
      note = "",
    } = req.body || {};

    if (!requestId || !isValidObjectId(requestId)) {
      return res.status(400).json({ message: "Valid requestId is required" });
    }

    const safeAmount = Number(amount || 0);
    if (!Number.isFinite(safeAmount) || safeAmount <= 0) {
      return res.status(400).json({ message: "Valid extra charge amount is required" });
    }

    if (!String(reason || "").trim()) {
      return res.status(400).json({ message: "Reason is required" });
    }

    const requestDoc = await Request.findById(requestId);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    const assignedProviderId = String(
      requestDoc.assignedProviderId || requestDoc.assignedTo || ""
    );
    const me = String(req.user._id || "");
    const isAdmin = role === "admin";

    if (!isAdmin && assignedProviderId !== me) {
      return res.status(403).json({ message: "You are not assigned to this request" });
    }

    const item = await ExtraCharge.create({
      requestId,
      providerId: requestDoc.assignedProviderId || requestDoc.assignedTo || req.user._id,
      amount: safeAmount,
      reason: String(reason).trim(),
      note: String(note || "").trim(),
      status: "pending",
    });

    emitExtraChargeUpdate(req.app, requestDoc, item);

    return res.status(201).json(item);
  } catch (err) {
    console.error("Create extra charge error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   ACCEPT EXTRA CHARGE (USER)
================================================= */
router.patch("/:id/accept", protect, async (req, res) => {
  try {
    const item = await ExtraCharge.findById(req.params.id);
    if (!item) {
      return res.status(404).json({ message: "Extra charge not found" });
    }

    const requestDoc = await Request.findById(item.requestId);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    const me = String(req.user?._id || "");
    const role = String(req.user?.role || "").toLowerCase();

    if (role !== "admin" && me !== String(requestDoc.userId || "")) {
      return res.status(403).json({ message: "Only the user can accept this extra charge" });
    }

    item.status = "accepted";
    await item.save();

    emitExtraChargeUpdate(req.app, requestDoc, item);

    return res.json(item);
  } catch (err) {
    console.error("Accept extra charge error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   REJECT EXTRA CHARGE (USER)
================================================= */
router.patch("/:id/reject", protect, async (req, res) => {
  try {
    const item = await ExtraCharge.findById(req.params.id);
    if (!item) {
      return res.status(404).json({ message: "Extra charge not found" });
    }

    const requestDoc = await Request.findById(item.requestId);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    const me = String(req.user?._id || "");
    const role = String(req.user?.role || "").toLowerCase();

    if (role !== "admin" && me !== String(requestDoc.userId || "")) {
      return res.status(403).json({ message: "Only the user can reject this extra charge" });
    }

    item.status = "rejected";
    await item.save();

    emitExtraChargeUpdate(req.app, requestDoc, item);

    return res.json(item);
  } catch (err) {
    console.error("Reject extra charge error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   PAY EXTRA CHARGE (USER)
================================================= */
router.patch("/:id/pay", protect, async (req, res) => {
  try {
    const item = await ExtraCharge.findById(req.params.id);
    if (!item) {
      return res.status(404).json({ message: "Extra charge not found" });
    }

    const requestDoc = await Request.findById(item.requestId);
    if (!requestDoc) {
      return res.status(404).json({ message: "Request not found" });
    }

    const me = String(req.user?._id || "");
    const role = String(req.user?.role || "").toLowerCase();

    if (role !== "admin" && me !== String(requestDoc.userId || "")) {
      return res.status(403).json({ message: "Only the user can pay this extra charge" });
    }

    const method = normalizeMethod(req.body?.method || "");
    if (!method) {
      return res.status(400).json({ message: "Valid payment method is required" });
    }

    if (method === "wallet") {
      const wallet = await getOrCreateUserWallet(req.user._id);
      if (Number(wallet.balance || 0) < Number(item.amount || 0)) {
        return res.status(400).json({ message: "Insufficient wallet balance" });
      }

      wallet.balance = Number(wallet.balance || 0) - Number(item.amount || 0);
      wallet.totalSpent = Number(wallet.totalSpent || 0) + Number(item.amount || 0);
      await wallet.save();

      const providerWallet = await getOrCreateProviderWallet(
        requestDoc.assignedProviderId || requestDoc.assignedTo
      );
      providerWallet.availableBalance =
        Number(providerWallet.availableBalance || 0) + Number(item.amount || 0);
      providerWallet.totalEarned =
        Number(providerWallet.totalEarned || 0) + Number(item.amount || 0);
      await providerWallet.save();

      await WalletTransaction.create({
        ownerType: "user",
        ownerId: req.user._id,
        transactionType: "service_payment",
        amount: Number(item.amount || 0),
        direction: "out",
        method: "wallet",
        requestId: requestDoc._id,
        reference: `EXTRA-${item._id}`,
        note: "Extra charge paid from wallet",
        status: "successful",
      });

      item.paymentMethod = "wallet";
      item.status = "paid";
      item.paidAt = new Date();
      await item.save();

      emitExtraChargeUpdate(req.app, requestDoc, item);

      return res.json(item);
    }

    if (method === "cash") {
      item.paymentMethod = "cash";
      item.status = "accepted";
      await item.save();

      emitExtraChargeUpdate(req.app, requestDoc, item);

      return res.json(item);
    }

    if (method === "airtel_money") {
      item.paymentMethod = "airtel_money";
      item.status = "paid";
      item.paidAt = new Date();
      await item.save();

      const providerWallet = await getOrCreateProviderWallet(
        requestDoc.assignedProviderId || requestDoc.assignedTo
      );
      providerWallet.availableBalance =
        Number(providerWallet.availableBalance || 0) + Number(item.amount || 0);
      providerWallet.totalEarned =
        Number(providerWallet.totalEarned || 0) + Number(item.amount || 0);
      await providerWallet.save();

      emitExtraChargeUpdate(req.app, requestDoc, item);

      return res.json(item);
    }

    return res.status(400).json({ message: "Unsupported payment method" });
  } catch (err) {
    console.error("Pay extra charge error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

export default router;