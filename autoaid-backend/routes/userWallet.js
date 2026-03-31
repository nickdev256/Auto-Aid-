import express from "express";
import { protect } from "../middleware/authMiddleware.js";
import UserWallet from "../models/UserWallet.js";
import WalletTransaction from "../models/WalletTransaction.js";

const router = express.Router();
const pendingDepositTimers = new Map();

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

async function completeDeposit(reference, app) {
  try {
    const tx = await WalletTransaction.findOne({
      reference,
      ownerType: "user",
      transactionType: "topup",
      method: "airtel_money",
    });

    if (!tx || tx.status === "successful") return;

    const wallet = await getOrCreateUserWallet(tx.ownerId);
    wallet.balance = Number(wallet.balance || 0) + Number(tx.amount || 0);
    wallet.totalTopUps = Number(wallet.totalTopUps || 0) + Number(tx.amount || 0);
    await wallet.save();

    tx.status = "successful";
    await tx.save();

    const io = app.get("io");
    if (io) {
      io.to(`user:${String(tx.ownerId)}`).emit("wallet_updated", {
        balance: wallet.balance,
        reference,
      });
    }
  } catch (err) {
    console.error("completeDeposit error:", err);
  } finally {
    pendingDepositTimers.delete(reference);
  }
}

function scheduleDeposit(reference, app) {
  if (pendingDepositTimers.has(reference)) {
    clearTimeout(pendingDepositTimers.get(reference));
  }

  const timer = setTimeout(() => {
    completeDeposit(reference, app);
  }, 4000);

  pendingDepositTimers.set(reference, timer);
}

router.get("/", protect, async (req, res) => {
  try {
    const wallet = await getOrCreateUserWallet(req.user._id);

    return res.json({
      balance: Number(wallet.balance || 0),
      totalTopUps: Number(wallet.totalTopUps || 0),
      totalSpent: Number(wallet.totalSpent || 0),
      totalRefunded: Number(wallet.totalRefunded || 0),
    });
  } catch (err) {
    console.error("Get user wallet error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

router.post("/deposit/initiate", protect, async (req, res) => {
  try {
    const { amount, phoneNumber, method = "airtel_money" } = req.body || {};
    const safeAmount = Number(amount || 0);

    if (!Number.isFinite(safeAmount) || safeAmount < 500) {
      return res.status(400).json({ message: "Minimum deposit is UGX 500" });
    }

    if (!String(phoneNumber || "").trim()) {
      return res.status(400).json({ message: "Phone number is required" });
    }

    if (String(method).trim().toLowerCase() !== "airtel_money") {
      return res.status(400).json({ message: "Only Airtel Money deposit is supported" });
    }

    const reference = `DEP-${Date.now()}`;

    await WalletTransaction.create({
      ownerType: "user",
      ownerId: req.user._id,
      transactionType: "topup",
      amount: safeAmount,
      direction: "in",
      method: "airtel_money",
      reference,
      note: `Wallet deposit initiated for ${String(phoneNumber).trim()}`,
      status: "pending",
    });

    scheduleDeposit(reference, req.app);

    return res.status(201).json({
      ok: true,
      message: "Check your phone to complete payment",
      status: "pending",
      amount: safeAmount,
      reference,
      phoneNumber: String(phoneNumber).trim(),
    });
  } catch (err) {
    console.error("Initiate wallet deposit error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

router.get("/deposit/:reference/status", protect, async (req, res) => {
  try {
    const { reference } = req.params;

    const tx = await WalletTransaction.findOne({
      reference,
      ownerType: "user",
      ownerId: req.user._id,
      transactionType: "topup",
      method: "airtel_money",
    });

    if (!tx) {
      return res.status(404).json({ message: "Deposit transaction not found" });
    }

    const wallet = await getOrCreateUserWallet(req.user._id);

    return res.json({
      ok: true,
      status: tx.status,
      amount: Number(tx.amount || 0),
      reference: tx.reference,
      balance: Number(wallet.balance || 0),
    });
  } catch (err) {
    console.error("Deposit status error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

router.patch("/deposit/:reference/verify", protect, async (req, res) => {
  try {
    const { reference } = req.params;
    await completeDeposit(reference, req.app);

    const wallet = await getOrCreateUserWallet(req.user._id);
    return res.json({
      ok: true,
      message: "Deposit successful",
      balance: Number(wallet.balance || 0),
      reference,
    });
  } catch (err) {
    console.error("Verify wallet deposit error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

export default router;