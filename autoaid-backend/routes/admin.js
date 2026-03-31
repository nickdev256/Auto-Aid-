import express from "express";
import User from "../models/User.js";
import Request from "../models/Request.js";
import Referral from "../models/Referral.js";
import Settings from "../models/Settings.js";
import { protect, authorize } from "../middleware/authMiddleware.js";

const router = express.Router();

console.log("✅ admin routes file loaded");

router.use(protect, authorize("admin"));

const toNumber = (value, fallback = 0) => {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
};

const normalizeStatus = (value, fallback = "unpaid") => {
  const s = String(value || fallback).trim().toLowerCase();
  return s || fallback;
};

const buildAbsoluteFileUrl = (req, filePath = "") => {
  if (!filePath) return "";
  const normalized = String(filePath).replace(/\\/g, "/").replace(/^\/+/, "");
  return `${req.protocol}://${req.get("host")}/${normalized}`;
};

router.get("/users", async (req, res) => {
  try {
    const from = String(req.query.from || "").toLowerCase();
    const role = String(req.query.role || "").toLowerCase();
    const verification = String(req.query.verification || "").toLowerCase();

    const filter = {};

    if (from) filter.registeredFrom = from;
    if (role) filter.role = role;
    if (verification) filter.verificationStatus = verification;

    const users = await User.find(filter)
      .select(`
        name
        email
        phone
        role
        status
        registeredFrom
        lastLoginFrom
        createdAt
        verificationStatus
        verificationDocumentType
        verificationDocumentUrl
        verificationSubmittedAt
        verificationReviewedAt
        verificationRejectionReason
        workLicenseDocumentUrl
        businessRegistrationDocumentUrl
        nationalIdFrontUrl
        nationalIdBackUrl
        profileImage
        referralCode
        referredBy
        hasUsedReferralDiscount
        nextReferralDiscountAmount
      `)
      .sort({ createdAt: -1 });

    res.json(users);
  } catch (err) {
    console.error("Admin users error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.get("/providers/pending", async (req, res) => {
  try {
    const pending = await User.find({
      role: "provider",
      status: "pending",
    })
      .select("-password")
      .sort({ createdAt: -1 });

    res.json(pending);
  } catch (err) {
    console.error("Pending providers error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.put("/approve/:id", async (req, res) => {
  try {
    const provider = await User.findById(req.params.id);

    if (!provider) {
      return res.status(404).json({ message: "Provider not found" });
    }

    provider.status = "approved";
    provider.isApprovedProvider = true;

    if (!provider.subscription) {
      provider.subscription = {
        plan: null,
        active: false,
        price: 0,
        startDate: null,
        expiryDate: null,
        paymentMethod: null,
      };
    }

    await provider.save();

    res.json({
      success: true,
      message: "Provider approved successfully",
      provider: provider.toObject(),
    });
  } catch (err) {
    console.error("Approve provider error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.put("/providers/:id/status", async (req, res) => {
  try {
    const nextStatus = String(req.body.status || "").trim().toLowerCase();

    if (!["pending", "approved", "rejected", "suspended"].includes(nextStatus)) {
      return res.status(400).json({ message: "Invalid provider status" });
    }

    const update = {
      status: nextStatus,
      isApprovedProvider: nextStatus === "approved",
    };

    const provider = await User.findOneAndUpdate(
      { _id: req.params.id, role: "provider" },
      update,
      { new: true }
    ).select("-password");

    if (!provider) {
      return res.status(404).json({ message: "Provider not found" });
    }

    res.json({
      message: "Provider status updated",
      provider,
    });
  } catch (err) {
    console.error("Update provider status error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.put("/subscribe/:id", async (req, res) => {
  try {
    let { plan } = req.body;

    const prices = {
      monthly: 10000,
      quarterly: 25000,
      yearly: 80000,
    };

    const daysMap = {
      monthly: 30,
      quarterly: 90,
      yearly: 365,
    };

    if (!plan) plan = "monthly";
    plan = String(plan).toLowerCase();

    if (!prices[plan]) {
      return res.status(400).json({ message: "Invalid plan selected" });
    }

    const user = await User.findById(req.params.id);
    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    const start = new Date();
    const expiry = new Date(start.getTime() + daysMap[plan] * 86400000);

    user.subscription = {
      plan,
      price: prices[plan],
      startDate: start,
      expiryDate: expiry,
      active: true,
      paymentMethod: "admin-activated",
    };

    await user.save();

    res.json({
      message: "Subscription activated",
      user,
    });
  } catch (err) {
    console.error("Subscribe error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.get("/providers", async (req, res) => {
  try {
    const page = Math.max(1, parseInt(req.query.page || "1", 10));
    const limit = Math.max(1, parseInt(req.query.limit || "20", 10));
    const search = String(req.query.search || "").trim();
    const status = req.query.status ? String(req.query.status).trim() : "";
    const subscribed = req.query.subscribed;
    const verificationStatus = req.query.verificationStatus
      ? String(req.query.verificationStatus).trim()
      : "";

    const filter = { role: "provider" };

    if (status) filter.status = status;
    if (verificationStatus) filter.verificationStatus = verificationStatus;

    if (search) {
      filter.$or = [
        { name: { $regex: search, $options: "i" } },
        { email: { $regex: search, $options: "i" } },
        { phone: { $regex: search, $options: "i" } },
        { businessName: { $regex: search, $options: "i" } },
      ];
    }

    if (typeof subscribed !== "undefined") {
      if (subscribed === "true") filter["subscription.active"] = true;
      if (subscribed === "false") filter["subscription.active"] = { $ne: true };
    }

    const total = await User.countDocuments(filter);

    const providers = await User.find(filter)
      .select("-password")
      .skip((page - 1) * limit)
      .limit(limit)
      .sort({ createdAt: -1 });

    const results = providers.map((p) => ({
      id: p._id,
      _id: p._id,
      name: p.name,
      businessName: p.businessName || p.name || "",
      email: p.email,
      phone: p.phone,
      status: p.status,
      isApprovedProvider: p.isApprovedProvider === true,
      registeredFrom: p.registeredFrom || "web",
      subscriptionActive: !!(p.subscription && p.subscription.active),
      subscriptionPlan: p.subscription?.plan || "None",
      subscriptionExpiry: p.subscription?.expiryDate || null,
      subscription: p.subscription || {
        active: false,
        plan: "",
        expiryDate: null,
      },
      businessType: p.businessType || null,
      providerType: p.providerType || p.businessType || null,
      address: p.address || null,
      createdAt: p.createdAt,
      providerVerification: {
        status: p.verificationStatus || "not_verified",
        licenseDocumentUrl: buildAbsoluteFileUrl(req, p.workLicenseDocumentUrl),
        businessDocumentUrl: buildAbsoluteFileUrl(
          req,
          p.businessRegistrationDocumentUrl
        ),
        nationalIdFrontUrl: buildAbsoluteFileUrl(req, p.nationalIdFrontUrl),
        nationalIdBackUrl: buildAbsoluteFileUrl(req, p.nationalIdBackUrl),
        profileImageUrl: buildAbsoluteFileUrl(req, p.profileImage),
        rejectionReason: p.verificationRejectionReason || "",
        submittedAt: p.verificationSubmittedAt || null,
        reviewedAt: p.verificationReviewedAt || null,
      },
    }));

    res.json({
      page,
      limit,
      total,
      pages: Math.ceil(total / limit),
      data: results,
    });
  } catch (err) {
    console.error("Get providers error:", err);
    res.status(500).json({ message: err.message || "Server error" });
  }
});

router.get("/providers/:id", async (req, res) => {
  try {
    const p = await User.findById(req.params.id).select("-password");

    if (!p) {
      return res.status(404).json({ message: "Provider not found" });
    }

    res.json({
      id: p._id,
      _id: p._id,
      name: p.name,
      businessName: p.businessName || p.name || "",
      email: p.email,
      phone: p.phone,
      status: p.status,
      isApprovedProvider: p.isApprovedProvider === true,
      registeredFrom: p.registeredFrom || "web",
      subscription: p.subscription || null,
      businessType: p.businessType || null,
      providerType: p.providerType || p.businessType || null,
      address: p.address || null,
      createdAt: p.createdAt,
      providerVerification: {
        status: p.verificationStatus || "not_verified",
        licenseDocumentUrl: buildAbsoluteFileUrl(req, p.workLicenseDocumentUrl),
        businessDocumentUrl: buildAbsoluteFileUrl(
          req,
          p.businessRegistrationDocumentUrl
        ),
        nationalIdFrontUrl: buildAbsoluteFileUrl(req, p.nationalIdFrontUrl),
        nationalIdBackUrl: buildAbsoluteFileUrl(req, p.nationalIdBackUrl),
        profileImageUrl: buildAbsoluteFileUrl(req, p.profileImage),
        rejectionReason: p.verificationRejectionReason || "",
        submittedAt: p.verificationSubmittedAt || null,
        reviewedAt: p.verificationReviewedAt || null,
      },
    });
  } catch (err) {
    console.error("Get provider error:", err);
    res.status(500).json({ message: err.message || "Server error" });
  }
});

router.patch("/providers/:id/verify", async (req, res) => {
  try {
    const provider = await User.findById(req.params.id);

    if (!provider) {
      return res.status(404).json({ message: "Provider not found" });
    }

    provider.verificationStatus = "verified";
    provider.verificationReviewedAt = new Date();
    provider.verificationRejectionReason = "";
    provider.isApprovedProvider = true;

    if (provider.status !== "approved") {
      provider.status = "approved";
    }

    await provider.save();

    res.json({
      message: "Provider verified successfully",
      provider,
    });
  } catch (err) {
    console.error("Verify provider error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.patch("/providers/:id/reject", async (req, res) => {
  try {
    const reason = String(req.body.reason || "").trim();

    if (!reason) {
      return res.status(400).json({ message: "Rejection reason required" });
    }

    const provider = await User.findById(req.params.id);

    if (!provider) {
      return res.status(404).json({ message: "Provider not found" });
    }

    provider.verificationStatus = "rejected";
    provider.verificationReviewedAt = new Date();
    provider.verificationRejectionReason = reason;
    provider.isApprovedProvider = false;
    provider.isAvailable = false;

    await provider.save();

    res.json({
      message: "Provider verification rejected",
      provider,
    });
  } catch (err) {
    console.error("Reject provider error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.get("/service-requests", async (req, res) => {
  try {
    const from = String(req.query.from || "").toLowerCase();
    const status = String(req.query.status || "").toLowerCase();

    const filter = {};

    if (from) filter.requestedFrom = from;
    if (status) filter.status = status;

    const requests = await Request.find(filter).sort({ createdAt: -1 });

    res.json(requests);
  } catch (err) {
    console.error("Admin service-requests error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.get("/service-requests/:id", async (req, res) => {
  try {
    const request = await Request.findById(req.params.id);

    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    res.json(request);
  } catch (err) {
    console.error("Get single request error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.patch("/service-requests/:id", async (req, res) => {
  try {
    const nextStatus = String(req.body.status || "").trim().toLowerCase();

    if (!nextStatus) {
      return res.status(400).json({ message: "status required" });
    }

    const request = await Request.findByIdAndUpdate(
      req.params.id,
      { status: nextStatus },
      { new: true }
    );

    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    res.json({
      message: "Updated",
      request,
    });
  } catch (err) {
    console.error("Update request error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.get("/revenue-dashboard", async (req, res) => {
  console.log("✅ /api/admin/revenue-dashboard route hit");

  try {
    const requests = await Request.find({}).sort({ createdAt: -1 });
    const subscribedProviders = await User.find({
      role: "provider",
      "subscription.active": true,
    }).select("name subscription");

    let totalSystemFees = 0;
    let escrowTotal = 0;
    let completedCount = 0;
    let awaitingConfirmationCount = 0;

    const revenueRequests = requests.map((r) => {
      const totalAmount = toNumber(
        r.totalAmount || r.amount || r.price || r.fee || 0,
        0
      );

      const systemFee =
        r.systemFee !== undefined && r.systemFee !== null
          ? toNumber(r.systemFee, 0)
          : totalAmount > 0
          ? Math.round(totalAmount * 0.1)
          : 0;

      const providerAmount =
        r.providerAmount !== undefined && r.providerAmount !== null
          ? toNumber(r.providerAmount, 0)
          : Math.max(0, totalAmount - systemFee);

      const paymentStatus = normalizeStatus(r.paymentStatus, "unpaid");
      const providerCompleted = !!r.providerCompleted;
      const userCompleted = !!r.userCompleted;

      if (systemFee > 0) totalSystemFees += systemFee;
      if (paymentStatus === "held_in_escrow") escrowTotal += totalAmount;

      if (providerCompleted && userCompleted) {
        completedCount += 1;
      } else if (providerCompleted || userCompleted) {
        awaitingConfirmationCount += 1;
      }

      return {
        _id: r._id,
        userName: r.userName || r.customerName || r.clientName || "-",
        assignedProviderName:
          r.assignedProviderName ||
          r.providerName ||
          r.assignedGarageName ||
          r.assignedFuelProviderName ||
          r.assignedTowingProviderName ||
          r.assignedAmbulanceProviderName ||
          "-",
        service:
          r.service || r.serviceType || r.category || r.providerType || "-",
        status: r.status || "-",
        totalAmount,
        systemFee,
        providerAmount,
        paymentStatus,
        paidAt: r.paidAt || null,
        createdAt: r.createdAt || null,
        providerCompleted,
        userCompleted,
        discountAmountApplied: toNumber(r.discountAmountApplied || 0, 0),
        discountTypeApplied: r.discountTypeApplied || null,
        finalQuoteAmount: toNumber(r.finalQuoteAmount || 0, 0),
      };
    });

    const totalSubscriptionRevenue = subscribedProviders.reduce((sum, provider) => {
      return sum + toNumber(provider?.subscription?.price || 0, 0);
    }, 0);

    const totalRevenue = totalSystemFees + totalSubscriptionRevenue;

    res.json({
      success: true,
      summary: {
        totalSystemFees,
        totalSubscriptionRevenue,
        totalRevenue,
        escrowTotal,
        completedCount,
        awaitingConfirmationCount,
      },
      requests: revenueRequests,
    });
  } catch (err) {
    console.error("Admin revenue dashboard error:", err);
    res.status(500).json({
      message: err.message || "Failed to load revenue dashboard",
    });
  }
});

router.get("/stats", async (req, res) => {
  try {
    const totalUsers = await User.countDocuments({ role: "user" });
    const totalProviders = await User.countDocuments({ role: "provider" });
    const totalAdmins = await User.countDocuments({ role: "admin" });

    const activeRequests = await Request.countDocuments({
      status: { $ne: "completed" },
    });

    const completedServices = await Request.countDocuments({
      status: "completed",
    });

    const pendingRequests = await Request.countDocuments({
      status: "pending",
    });

    const webRequests = await Request.countDocuments({
      requestedFrom: "web",
    });

    const androidRequests = await Request.countDocuments({
      requestedFrom: "android",
    });

    const approvedProviders = await User.countDocuments({
      role: "provider",
      status: "approved",
    });

    const pendingProviders = await User.countDocuments({
      role: "provider",
      status: "pending",
    });

    const subscribedProviders = await User.countDocuments({
      role: "provider",
      "subscription.active": true,
    });

    const pendingVerificationUsers = await User.countDocuments({
      verificationStatus: "pending",
    });

    const verifiedUsers = await User.countDocuments({
      verificationStatus: "verified",
    });

    const rejectedVerificationUsers = await User.countDocuments({
      verificationStatus: "rejected",
    });

    const totalReferrals = await Referral.countDocuments({});
    const rewardedReferrals = await Referral.countDocuments({ status: "rewarded" });
    const signedUpReferrals = await Referral.countDocuments({ status: "signed_up" });

    res.json({
      totalUsers,
      totalProviders,
      totalAdmins,
      approvedProviders,
      pendingProviders,
      subscribedProviders,
      activeRequests,
      completedServices,
      pendingRequests,
      webRequests,
      androidRequests,
      pendingVerificationUsers,
      verifiedUsers,
      rejectedVerificationUsers,
      totalReferrals,
      rewardedReferrals,
      signedUpReferrals,
    });
  } catch (err) {
    console.error("Admin stats error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.get("/referrals", async (req, res) => {
  try {
    const referrals = await Referral.find({})
      .populate("referrerUserId", "name email")
      .populate("referredUserId", "name email")
      .sort({ createdAt: -1 });

    res.json({
      total: referrals.length,
      data: referrals.map((r) => ({
        _id: r._id,
        referrer: r.referrerUserId
          ? {
              _id: r.referrerUserId._id,
              name: r.referrerUserId.name,
              email: r.referrerUserId.email,
            }
          : null,
        referredUser: r.referredUserId
          ? {
              _id: r.referredUserId._id,
              name: r.referredUserId.name,
              email: r.referredUserId.email,
            }
          : null,
        referralCode: r.referralCode,
        status: r.status,
        friendDiscountAmount: r.friendDiscountAmount,
        referrerRewardAmount: r.referrerRewardAmount,
        qualifyingRequestId: r.qualifyingRequestId,
        rewardedAt: r.rewardedAt,
        createdAt: r.createdAt,
      })),
    });
  } catch (err) {
    console.error("Admin referrals error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.get("/settings", async (req, res) => {
  try {
    let data = await Settings.findOne();

    if (!data) {
      data = await Settings.create({
        systemName: "AutoAid",
        supportEmail: "",
        notificationsEnabled: true,
        maintenanceMode: false,
        maintenanceMessage:
          "AutoAid is currently under maintenance. Please try again later.",
      });
    }

    res.json(data);
  } catch (err) {
    console.error("Get settings error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.put("/settings", async (req, res) => {
  try {
    let data = await Settings.findOne();

    if (!data) {
      data = new Settings();
    }

    const {
      systemName,
      supportEmail,
      notificationsEnabled,
      maintenanceMode,
      maintenanceMessage,
    } = req.body;

    if (typeof systemName !== "undefined") {
      data.systemName = systemName;
    }

    if (typeof supportEmail !== "undefined") {
      data.supportEmail = supportEmail;
    }

    if (typeof notificationsEnabled !== "undefined") {
      data.notificationsEnabled = notificationsEnabled;
    }

    if (typeof maintenanceMode !== "undefined") {
      data.maintenanceMode = maintenanceMode;
    }

    if (typeof maintenanceMessage !== "undefined") {
      data.maintenanceMessage = maintenanceMessage;
    }

    await data.save();

    res.json({
      message: "Settings updated",
      data,
    });
  } catch (err) {
    console.error("Update settings error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.patch("/settings/maintenance", async (req, res) => {
  try {
    let data = await Settings.findOne();

    if (!data) {
      data = new Settings({
        systemName: "AutoAid",
        supportEmail: "",
        notificationsEnabled: true,
        maintenanceMode: false,
        maintenanceMessage:
          "AutoAid is currently under maintenance. Please try again later.",
      });
    }

    if (typeof req.body.maintenanceMode === "undefined") {
      return res.status(400).json({ message: "maintenanceMode is required" });
    }

    data.maintenanceMode = !!req.body.maintenanceMode;

    if (typeof req.body.maintenanceMessage !== "undefined") {
      data.maintenanceMessage = req.body.maintenanceMessage;
    }

    await data.save();

    res.json({
      message: `Maintenance mode ${data.maintenanceMode ? "enabled" : "disabled"}`,
      data,
    });
  } catch (err) {
    console.error("Maintenance toggle error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

router.get("/pending-providers", async (req, res) => {
  try {
    const pending = await User.find({
      role: "provider",
      status: "pending",
    }).select("-password");

    res.json(pending);
  } catch (err) {
    console.error("Pending providers legacy route error:", err);
    res.status(500).json({ error: "Server error" });
  }
});

export default router;