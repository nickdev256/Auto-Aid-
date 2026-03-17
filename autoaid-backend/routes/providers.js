// ✅ FILE NAME: routes/providers.js
import express from "express";
import mongoose from "mongoose";
import User from "../models/User.js";
import Request from "../models/Request.js";
import PayoutRequest from "../models/PayoutRequest.js";
import { protect, authorize } from "../middleware/authMiddleware.js";

const router = express.Router();

/* =================================================
   HELPERS
================================================= */
function ensureObjectId(id) {
  return mongoose.Types.ObjectId.isValid(id);
}

function normalizeBusinessType(value = "") {
  const v = String(value || "").trim().toLowerCase();

  if (["garage", "fuel", "towing", "ambulance"].includes(v)) {
    return v;
  }

  return "";
}

function toNumberOrNull(value) {
  if (value === undefined || value === null || value === "") return null;
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

function toRadians(value) {
  return (value * Math.PI) / 180;
}

function calculateDistanceKm(lat1, lng1, lat2, lng2) {
  const safeLat1 = Number(lat1);
  const safeLng1 = Number(lng1);
  const safeLat2 = Number(lat2);
  const safeLng2 = Number(lng2);

  if (
    !Number.isFinite(safeLat1) ||
    !Number.isFinite(safeLng1) ||
    !Number.isFinite(safeLat2) ||
    !Number.isFinite(safeLng2)
  ) {
    return null;
  }

  const earthRadiusKm = 6371;
  const dLat = toRadians(safeLat2 - safeLat1);
  const dLng = toRadians(safeLng2 - safeLng1);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(safeLat1)) *
      Math.cos(toRadians(safeLat2)) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Number((earthRadiusKm * c).toFixed(2));
}

function getProviderRaw(doc) {
  if (!doc) return {};
  if (typeof doc.getDecrypted === "function") return doc.getDecrypted();
  if (typeof doc.toObject === "function") return doc.toObject();
  return doc;
}

function getProviderType(raw = {}) {
  return String(raw.businessType || raw.providerType || "").trim().toLowerCase();
}

function formatProvider(providerDoc, userLat = null, userLng = null) {
  const raw = getProviderRaw(providerDoc);

  const providerId = raw._id ? String(raw._id) : "";
  const providerLat = toNumberOrNull(raw.lat);
  const providerLng = toNumberOrNull(raw.lng);

  const hasValidUserLocation =
    userLat !== null &&
    userLng !== null &&
    Number.isFinite(Number(userLat)) &&
    Number.isFinite(Number(userLng));

  const distanceKm =
    hasValidUserLocation && providerLat !== null && providerLng !== null
      ? calculateDistanceKm(Number(userLat), Number(userLng), providerLat, providerLng)
      : null;

  return {
    id: providerId,
    _id: providerId,
    name: raw.name || "",
    phone: raw.phone || "",
    businessName: raw.businessName || "",
    businessType: getProviderType(raw),
    providerType: getProviderType(raw),
    servicesOffered: Array.isArray(raw.servicesOffered) ? raw.servicesOffered : [],
    address: raw.address || "",
    rating: Number(raw.rating || 0),
    lat: providerLat,
    lng: providerLng,
    isAvailable: !!raw.isAvailable,
    isOnline: !!raw.isOnline,
    isApprovedProvider: !!raw.isApprovedProvider,
    profileImageUrl: raw.profileImageUrl || "",
    logoUrl: raw.logoUrl || "",
    subscription: raw.subscription || {},
    payoutInfo: raw.payoutInfo || {},
    status: raw.status || "",
    distanceKm,
  };
}

function buildProviderTypeQuery(providerType) {
  return {
    $or: [{ businessType: providerType }, { providerType: providerType }],
  };
}

function sortProviders(list, lat, lng) {
  const hasValidUserLocation =
    lat !== null &&
    lng !== null &&
    Number.isFinite(lat) &&
    Number.isFinite(lng);

  if (hasValidUserLocation) {
    return list.sort((a, b) => {
      const aDistance = a.distanceKm ?? Number.MAX_SAFE_INTEGER;
      const bDistance = b.distanceKm ?? Number.MAX_SAFE_INTEGER;

      if (aDistance !== bDistance) return aDistance - bDistance;
      return (b.rating || 0) - (a.rating || 0);
    });
  }

  return list.sort((a, b) => (b.rating || 0) - (a.rating || 0));
}

function getRequestProviderEarning(requestDoc) {
  return (
    Number(requestDoc?.providerAmount) ||
    Number(requestDoc?.agreedAmount) ||
    Number(requestDoc?.finalAmount) ||
    Number(requestDoc?.paymentAmount) ||
    Number(requestDoc?.quoteAmount) ||
    Number(requestDoc?.quotedAmount) ||
    Number(requestDoc?.totalAmount) ||
    Number(requestDoc?.amount) ||
    Number(requestDoc?.price) ||
    0
  );
}

async function computeProviderWalletSummary(providerIdInput) {
  const providerId = new mongoose.Types.ObjectId(providerIdInput);

  const paidCompletedRequests = await Request.find({
    $and: [
      {
        $or: [
          { assignedProviderId: providerId },
          { assignedTo: providerId },
        ],
      },
      { status: "completed" },
      { paymentStatus: "paid" },
    ],
  }).select(
    "providerAmount agreedAmount finalAmount paymentAmount quoteAmount quotedAmount totalAmount amount price"
  );

  const totalEarned = paidCompletedRequests.reduce(
    (sum, r) => sum + getRequestProviderEarning(r),
    0
  );

  const [pendingAgg, paidAgg] = await Promise.all([
    PayoutRequest.aggregate([
      {
        $match: {
          providerId,
          status: { $in: ["pending", "approved"] },
        },
      },
      {
        $group: {
          _id: null,
          total: { $sum: "$amount" },
        },
      },
    ]),
    PayoutRequest.aggregate([
      {
        $match: {
          providerId,
          status: "paid",
        },
      },
      {
        $group: {
          _id: null,
          total: { $sum: "$amount" },
        },
      },
    ]),
  ]);

  const pendingBalance = pendingAgg[0]?.total || 0;
  const totalPaidOut = paidAgg[0]?.total || 0;
  const availableBalance = Math.max(0, totalEarned - pendingBalance - totalPaidOut);

  return {
    totalEarned,
    pendingBalance,
    totalPaidOut,
    availableBalance,
    paidCompletedRequestsCount: paidCompletedRequests.length,
  };
}

/* =================================================
   1️⃣ PUBLIC ROUTES (NO AUTH REQUIRED)
================================================= */

router.get("/garages/approved", async (req, res) => {
  try {
    const garages = await User.find(
      {
        role: "provider",
        ...buildProviderTypeQuery("garage"),
        "subscription.active": true,
        status: { $in: ["approved", "active"] },
        isApprovedProvider: true,
      },
      "-password"
    );

    return res.json({ garages: garages.map((g) => formatProvider(g)) });
  } catch (err) {
    console.error("Get approved garages error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

router.get("/public/:id", async (req, res) => {
  try {
    const { id } = req.params;

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid provider id" });
    }

    const provider = await User.findById(id).select(
      "name businessName businessType providerType servicesOffered address phone logoUrl subscription payoutInfo rating lat lng isAvailable isOnline isApprovedProvider profileImageUrl status"
    );

    if (!provider) {
      return res.status(404).json({ message: "Provider not found" });
    }

    return res.json(formatProvider(provider));
  } catch (err) {
    console.error("Public provider profile error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

router.get("/:id/subscription", async (req, res) => {
  try {
    const { id } = req.params;

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid provider id" });
    }

    const provider = await User.findById(id).select("subscription role status");
    if (!provider) {
      return res.status(404).json({ message: "Provider not found" });
    }

    if (provider.role !== "provider") {
      return res.status(400).json({ message: "Not a provider account" });
    }

    return res.json({
      subscription: provider.subscription || {},
      status: provider.subscription?.active ? "active" : "inactive",
    });
  } catch (err) {
    console.error("Provider subscription error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   AVAILABLE PROVIDERS FOR USER CHOICE
   GET /api/providers/available?providerType=garage&lat=...&lng=...
================================================= */
router.get("/available", async (req, res) => {
  try {
    const providerType = normalizeBusinessType(
      req.query.providerType || req.query.service
    );

    const lat = toNumberOrNull(req.query.lat);
    const lng = toNumberOrNull(req.query.lng);
    const onlineOnly =
      String(req.query.onlineOnly || "true").trim().toLowerCase() !== "false";

    if (!providerType) {
      return res.status(400).json({
        message:
          "Valid providerType is required (garage, fuel, towing, ambulance)",
      });
    }

    const query = {
      role: "provider",
      ...buildProviderTypeQuery(providerType),
      isApprovedProvider: true,
      status: { $in: ["approved", "active"] },
    };

    if (onlineOnly) {
      query.isOnline = true;
    }

    const providers = await User.find(query).select(
      "name phone businessName businessType providerType servicesOffered address rating profileImageUrl logoUrl payoutInfo subscription lat lng isAvailable isOnline isApprovedProvider status"
    );

    let list = providers.map((p) => formatProvider(p, lat, lng));
    list = sortProviders(list, lat, lng);

    return res.json(list);
  } catch (err) {
    console.error("Get available providers error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* =================================================
   🔒 ADMIN ROUTES
   MUST COME BEFORE provider-only middleware
================================================= */
router.get(
  "/admin/payout-requests",
  protect,
  authorize("admin"),
  async (req, res) => {
    try {
      const list = await PayoutRequest.find()
        .populate("providerId", "name email businessName phone")
        .sort({ createdAt: -1 });

      return res.json(list);
    } catch (err) {
      console.error("Admin get payout requests error:", err);
      return res.status(500).json({ message: "Server error" });
    }
  }
);

router.patch(
  "/admin/payout-requests/:id/approve",
  protect,
  authorize("admin"),
  async (req, res) => {
    try {
      const item = await PayoutRequest.findById(req.params.id);
      if (!item) {
        return res.status(404).json({ message: "Payout request not found" });
      }

      item.status = "approved";
      item.adminNote = req.body?.adminNote || "";
      await item.save();

      return res.json(item);
    } catch (err) {
      console.error("Admin approve payout error:", err);
      return res.status(500).json({ message: "Server error" });
    }
  }
);

router.patch(
  "/admin/payout-requests/:id/reject",
  protect,
  authorize("admin"),
  async (req, res) => {
    try {
      const item = await PayoutRequest.findById(req.params.id);
      if (!item) {
        return res.status(404).json({ message: "Payout request not found" });
      }

      item.status = "rejected";
      item.adminNote = req.body?.adminNote || "";
      await item.save();

      return res.json(item);
    } catch (err) {
      console.error("Admin reject payout error:", err);
      return res.status(500).json({ message: "Server error" });
    }
  }
);

router.patch(
  "/admin/payout-requests/:id/paid",
  protect,
  authorize("admin"),
  async (req, res) => {
    try {
      const item = await PayoutRequest.findById(req.params.id);
      if (!item) {
        return res.status(404).json({ message: "Payout request not found" });
      }

      item.status = "paid";
      item.paidAt = new Date();
      item.adminNote = req.body?.adminNote || item.adminNote || "";
      await item.save();

      return res.json(item);
    } catch (err) {
      console.error("Admin mark payout paid error:", err);
      return res.status(500).json({ message: "Server error" });
    }
  }
);

/* =================================================
   🔒 PROVIDER-ONLY ROUTES
================================================= */
router.use(protect, authorize("provider"));

router.get("/me", async (req, res) => {
  try {
    const providerDoc = await User.findById(req.user._id).select("-password");
    if (!providerDoc) {
      return res.status(404).json({ message: "Provider not found" });
    }

    return res.json(getProviderRaw(providerDoc));
  } catch (err) {
    console.error("Provider me error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

router.patch("/me", async (req, res) => {
  try {
    const updates = { ...(req.body || {}) };

    delete updates._id;
    delete updates.id;

    const providerDoc = await User.findByIdAndUpdate(req.user._id, updates, {
      new: true,
      runValidators: true,
    }).select("-password");

    if (!providerDoc) {
      return res.status(404).json({ message: "Provider not found" });
    }

    return res.json(getProviderRaw(providerDoc));
  } catch (err) {
    console.error("Provider update me error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* -----------------------------------------
   PROVIDER AVAILABILITY
------------------------------------------ */
router.patch("/availability", async (req, res) => {
  try {
    const { isAvailable, lat, lng } = req.body || {};

    const update = {
      isAvailable: !!isAvailable,
    };

    const safeLat = toNumberOrNull(lat);
    const safeLng = toNumberOrNull(lng);

    if (safeLat !== null) update.lat = safeLat;
    if (safeLng !== null) update.lng = safeLng;

    const providerDoc = await User.findByIdAndUpdate(
      req.user._id,
      { $set: update },
      { new: true, runValidators: true }
    ).select("-password");

    if (!providerDoc) {
      return res.status(404).json({ message: "Provider not found" });
    }

    return res.json({
      message: "Availability updated successfully",
      provider: getProviderRaw(providerDoc),
    });
  } catch (err) {
    console.error("Provider availability update error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* -----------------------------------------
   PROVIDER PAYOUT INFO
------------------------------------------ */
router.get("/payout-info", async (req, res) => {
  try {
    const providerDoc = await User.findById(req.user._id).select("payoutInfo");
    if (!providerDoc) {
      return res.status(404).json({ message: "Provider not found" });
    }

    const decrypted = getProviderRaw(providerDoc);
    return res.json(decrypted.payoutInfo || {});
  } catch (err) {
    console.error("Get payout info error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

router.patch("/payout-info", async (req, res) => {
  try {
    const {
      method,
      accountName = "",
      phoneNumber = "",
      bankName = "",
      accountNumber = "",
    } = req.body || {};

    if (!method || !["mobile_money", "bank"].includes(method)) {
      return res.status(400).json({ message: "Valid payout method is required" });
    }

    if (!String(accountName).trim()) {
      return res.status(400).json({ message: "Account name is required" });
    }

    if (method === "mobile_money" && !String(phoneNumber).trim()) {
      return res.status(400).json({ message: "Phone number is required" });
    }

    if (
      method === "bank" &&
      (!String(bankName).trim() || !String(accountNumber).trim())
    ) {
      return res.status(400).json({
        message: "Bank name and account number are required",
      });
    }

    const providerDoc = await User.findById(req.user._id);
    if (!providerDoc) {
      return res.status(404).json({ message: "Provider not found" });
    }

    providerDoc.payoutInfo = {
      method,
      accountName: String(accountName).trim(),
      phoneNumber: method === "mobile_money" ? String(phoneNumber).trim() : "",
      bankName: method === "bank" ? String(bankName).trim() : "",
      accountNumber: method === "bank" ? String(accountNumber).trim() : "",
      isVerified: false,
    };

    await providerDoc.save();

    return res.json({
      message: "Payout info updated successfully",
      payoutInfo: getProviderRaw(providerDoc).payoutInfo,
    });
  } catch (err) {
    console.error("Update payout info error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* -----------------------------------------
   PROVIDER WALLET SUMMARY
------------------------------------------ */
router.get("/wallet", async (req, res) => {
  try {
    const summary = await computeProviderWalletSummary(req.user._id);

    return res.json({
      totalEarned: summary.totalEarned,
      pendingBalance: summary.pendingBalance,
      totalPaidOut: summary.totalPaidOut,
      availableBalance: summary.availableBalance,
    });
  } catch (err) {
    console.error("Get provider wallet error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* -----------------------------------------
   CREATE PAYOUT REQUEST
------------------------------------------ */
router.post("/payout-requests", async (req, res) => {
  try {
    const providerDoc = await User.findById(req.user._id);
    if (!providerDoc) {
      return res.status(404).json({ message: "Provider not found" });
    }

    const payoutInfo = getProviderRaw(providerDoc).payoutInfo || {};
    if (!payoutInfo.accountName || !payoutInfo.method) {
      return res.status(400).json({ message: "Please add payout information first" });
    }

    const amount = Number(req.body?.amount || 0);

    if (!amount || amount <= 0) {
      return res.status(400).json({ message: "Valid amount is required" });
    }

    const summary = await computeProviderWalletSummary(req.user._id);

    if (amount > summary.availableBalance) {
      return res.status(400).json({
        message: "Requested amount exceeds available balance",
        availableBalance: summary.availableBalance,
      });
    }

    const payoutRequest = await PayoutRequest.create({
      providerId: new mongoose.Types.ObjectId(req.user._id),
      amount,
      method: payoutInfo.method,
      accountName: payoutInfo.accountName || "",
      phoneNumber: payoutInfo.phoneNumber || "",
      bankName: payoutInfo.bankName || "",
      accountNumber: payoutInfo.accountNumber || "",
      status: "pending",
    });

    return res.status(201).json(payoutRequest);
  } catch (err) {
    console.error("Create payout request error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* -----------------------------------------
   PROVIDER PAYOUT HISTORY
------------------------------------------ */
router.get("/payout-requests", async (req, res) => {
  try {
    const payoutRequests = await PayoutRequest.find({
      providerId: new mongoose.Types.ObjectId(req.user._id),
    }).sort({ createdAt: -1 });

    return res.json(payoutRequests);
  } catch (err) {
    console.error("Get provider payout requests error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* -----------------------------------------
   LEGACY SETTINGS ROUTE
------------------------------------------ */
router.put("/provider/:id/settings", async (req, res) => {
  try {
    const { id } = req.params;

    if (!ensureObjectId(id)) {
      return res.status(400).json({ message: "Invalid provider id" });
    }

    if (req.user._id.toString() !== id) {
      return res.status(403).json({ message: "Access denied" });
    }

    const updates = { ...(req.body || {}) };
    delete updates._id;
    delete updates.id;

    const providerDoc = await User.findByIdAndUpdate(id, updates, {
      new: true,
      runValidators: true,
    }).select("-password");

    if (!providerDoc) {
      return res.status(404).json({ message: "Provider not found" });
    }

    return res.json(getProviderRaw(providerDoc));
  } catch (err) {
    console.error("Provider settings update error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* -----------------------------------------
   PROVIDER REQUESTS
------------------------------------------ */
router.get("/requests/byProvider/:providerId", async (req, res) => {
  try {
    const { providerId } = req.params;

    if (req.user._id.toString() !== providerId) {
      return res.status(403).json({ message: "Access denied" });
    }

    if (!ensureObjectId(providerId)) {
      return res.status(400).json({ message: "Invalid provider id" });
    }

    const provider = await User.findById(providerId);
    if (!provider) {
      return res.status(404).json({ message: "Provider not found" });
    }

    const rawProvider = getProviderRaw(provider);
    const normalizedType = getProviderType(rawProvider);

    const requests = await Request.find({
      $or: [
        { providerType: normalizedType },
        { assignedProviderId: provider._id },
        { targetProviderId: provider._id },
      ],
    }).sort({ createdAt: -1 });

    return res.json(requests);
  } catch (err) {
    console.error("Provider requests error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

router.get("/garageRequests/byProvider/:providerId", async (req, res) => {
  req.url = `/requests/byProvider/${req.params.providerId}`;
  router.handle(req, res);
});

export default router;