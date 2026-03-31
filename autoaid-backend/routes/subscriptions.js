import express from "express";
import mongoose from "mongoose";
import ProviderSubscription from "../models/ProviderSubscription.js";
import User from "../models/User.js";
import { protect } from "../middleware/authMiddleware.js";

const router = express.Router();

const PLANS = [
  { id: "monthly", name: "Monthly", price: 50000, durationDays: 30 },
  { id: "quarterly", name: "Quarterly", price: 100000, durationDays: 90 },
  { id: "yearly", name: "Yearly", price: 250000, durationDays: 365 },
];

function isValidObjectId(id) {
  return mongoose.Types.ObjectId.isValid(id);
}

function getPlan(planId) {
  return PLANS.find((p) => p.id === String(planId || "").trim().toLowerCase());
}

function addDays(date, days) {
  const d = new Date(date);
  d.setDate(d.getDate() + Number(days || 0));
  return d;
}

router.get("/plans", async (_req, res) => {
  return res.json({
    ok: true,
    plans: PLANS,
  });
});

router.get("/provider/:providerId", async (req, res) => {
  try {
    const { providerId } = req.params;

    if (!isValidObjectId(providerId)) {
      return res.status(400).json({
        ok: false,
        message: "Invalid providerId",
      });
    }

    const sub = await ProviderSubscription.findOne({ providerId }).lean();

    if (!sub) {
      return res.json({
        ok: true,
        subscription: {
          active: false,
          plan: null,
          expiryDate: null,
          paymentStatus: "unpaid",
        },
      });
    }

    const now = new Date();
    const active = !!sub.active && sub.expiryDate && new Date(sub.expiryDate) > now;

    return res.json({
      ok: true,
      subscription: {
        active,
        plan: sub.planName,
        planId: sub.planId,
        amount: sub.amount,
        expiryDate: sub.expiryDate,
        paymentStatus: sub.paymentStatus,
        phoneNumber: sub.phoneNumber,
        network: sub.network,
        paymentReference: sub.paymentReference,
      },
    });
  } catch (err) {
    console.error("Get provider subscription error:", err);
    return res.status(500).json({
      ok: false,
      message: "Server error",
    });
  }
});

router.post("/subscribe", async (req, res) => {
  try {
    const { providerId, planId, phone, network } = req.body || {};

    if (!providerId || !isValidObjectId(providerId)) {
      return res.status(400).json({
        ok: false,
        message: "Valid providerId is required",
      });
    }

    const plan = getPlan(planId);
    if (!plan) {
      return res.status(400).json({
        ok: false,
        message: "Invalid plan selected",
      });
    }

    if (!phone || !String(phone).trim()) {
      return res.status(400).json({
        ok: false,
        message: "Phone number is required",
      });
    }

    const provider = await User.findById(providerId);
    if (!provider) {
      return res.status(404).json({
        ok: false,
        message: "Provider not found",
      });
    }

    const now = new Date();
    const expiryDate = addDays(now, plan.durationDays);
    const paymentReference = `SUB-${String(providerId).slice(-6)}-${Date.now()}`;

    const updated = await ProviderSubscription.findOneAndUpdate(
      { providerId },
      {
        $set: {
          providerId,
          planId: plan.id,
          planName: plan.name,
          amount: plan.price,
          durationDays: plan.durationDays,
          active: true,
          paymentStatus: "paid",
          paymentMethod: "mobile_money",
          phoneNumber: String(phone).trim(),
          network: String(network || "").trim().toLowerCase(),
          startedAt: now,
          expiryDate,
          lastPaymentAt: now,
          paymentReference,
        },
      },
      {
        new: true,
        upsert: true,
        setDefaultsOnInsert: true,
      }
    );

    if ("isApproved" in provider) {
      provider.isApproved = true;
    }
    if ("subscriptionActive" in provider) {
      provider.subscriptionActive = true;
    }
    if ("subscriptionPlan" in provider) {
      provider.subscriptionPlan = plan.name;
    }
    if ("subscriptionExpiryDate" in provider) {
      provider.subscriptionExpiryDate = expiryDate;
    }

    await provider.save();

    return res.status(201).json({
      ok: true,
      success: true,
      message: "Subscription activated successfully",
      sandboxMode: true,
      subscription: {
        active: true,
        plan: updated.planName,
        planId: updated.planId,
        amount: updated.amount,
        expiryDate: updated.expiryDate,
        paymentStatus: updated.paymentStatus,
        paymentReference: updated.paymentReference,
      },
    });
  } catch (err) {
    console.error("Subscribe provider error:", err);
    return res.status(500).json({
      ok: false,
      message: "Server error",
    });
  }
});

export default router;