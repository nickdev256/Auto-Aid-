import jwt from "jsonwebtoken";
import bcrypt from "bcryptjs";
import User from "../models/User.js";
import Referral from "../models/Referral.js";
import Settings from "../models/Settings.js";
import { signToken } from "../utils/jwt.js";
import { sendResetEmail } from "../utils/sendResetEmail.js";
import {
  generateUniqueReferralCode,
  REFERRAL_DISCOUNT_AMOUNT,
} from "../utils/referral.js";

function isMobileClient(req) {
  const client = (req.headers["x-client"] || "").toString().toLowerCase();
  return client === "mobile" || client === "android";
}

function buildAuthUser(userDoc) {
  const user =
    typeof userDoc.getDecrypted === "function"
      ? userDoc.getDecrypted()
      : userDoc;

  return {
    id: user._id,
    _id: user._id,
    name: user.name,
    email: user.email,
    phone: user.phone || "",
    role: user.role,
    status: user.status,

    // IMPORTANT: send provider type to frontend
    businessType: user.businessType || "",
    services: Array.isArray(user.services) ? user.services : [],
    businessName: user.businessName || "",
    address: user.address || "",
    description: user.description || "",
    isOnline: user.isOnline ?? false,
    isVerified: !!user.isVerified,

    verificationStatus: user.verificationStatus || "not_verified",
    registeredFrom: user.registeredFrom || "web",
    lastLoginFrom: user.lastLoginFrom || "web",

    // referral
    referralCode: user.referralCode || "",
    referredBy: user.referredBy || null,
    hasUsedReferralDiscount: !!user.hasUsedReferralDiscount,
    nextReferralDiscountAmount: Number(user.nextReferralDiscountAmount || 0),
  };
}

async function getSystemSettings() {
  let settings = await Settings.findOne();

  if (!settings) {
    settings = await Settings.create({
      systemName: "AutoAid",
      supportEmail: "",
      supportPhone: "",
      whatsappNumber: "",
      emergencyHotline: "",
      notificationsEnabled: true,
      maintenanceMode: false,
      maintenanceMessage:
        "AutoAid is currently under maintenance. Please try again later.",
      maintenanceTarget: "both",
      allowUserRegistration: true,
      allowProviderRegistration: true,
      autoApproveProviders: false,
    });
  }

  return settings;
}

export async function signup(req, res) {
  try {
    const {
      name,
      email,
      password,
      role,
      phone,
      referralCode,
      businessType,
      businessName,
      address,
      services,
      description,
    } = req.body || {};

    if (!name || !email || !password) {
      return res.status(400).json({
        message: "Name, email, password are required",
      });
    }

    const cleanEmail = String(email).toLowerCase().trim();
    const cleanName = String(name).trim();
    const cleanPhone = String(phone || "").trim();
    const cleanReferralCode = String(referralCode || "")
      .trim()
      .toUpperCase();
    const cleanBusinessType = String(businessType || "")
      .trim()
      .toLowerCase();

    const exists = await User.findOne({ email: cleanEmail });
    if (exists) {
      return res.status(409).json({
        message: "Email already exists",
      });
    }

    const settings = await getSystemSettings();

    const newRole =
      role && ["user", "provider"].includes(role) ? role : "user";

    if (newRole === "user" && settings.allowUserRegistration === false) {
      return res.status(403).json({
        message: "User registration is currently disabled.",
      });
    }

    if (newRole === "provider" && settings.allowProviderRegistration === false) {
      return res.status(403).json({
        message: "Provider registration is currently disabled.",
      });
    }

    if (
      newRole === "provider" &&
      cleanBusinessType &&
      !["garage", "fuel", "towing", "ambulance"].includes(cleanBusinessType)
    ) {
      return res.status(400).json({
        message: "Invalid business type for provider account.",
      });
    }

    let status = "active";

    if (newRole === "provider") {
      status = settings.autoApproveProviders ? "approved" : "pending";
    }

    let referrer = null;

    if (cleanReferralCode) {
      referrer = await User.findOne({ referralCode: cleanReferralCode });

      if (!referrer) {
        return res.status(400).json({
          message: "Invalid referral code",
        });
      }
    }

    const clientSource = isMobileClient(req) ? "android" : "web";
    const ownReferralCode = await generateUniqueReferralCode(cleanName);

    const payload = {
      name: cleanName,
      email: cleanEmail,
      password,
      phone: cleanPhone,
      role: newRole,
      status,
      verificationStatus: "not_verified",
      registeredFrom: clientSource,
      lastLoginFrom: clientSource,

      referralCode: ownReferralCode,
      referredBy: referrer ? referrer._id : null,
      referralCodeUsedAtSignup: referrer ? cleanReferralCode : null,
      hasUsedReferralDiscount: false,
      nextReferralDiscountAmount: 0,
    };

    if (newRole === "provider") {
      payload.businessType = cleanBusinessType || "";
      payload.businessName = String(businessName || cleanName).trim();
      payload.address = String(address || "").trim();
      payload.description = String(description || "").trim();
      payload.services = Array.isArray(services) ? services : [];
      payload.isOnline = false;
      payload.isVerified = false;
    }

    const user = await new User(payload).save();

    if (referrer && String(referrer._id) !== String(user._id)) {
      await Referral.create({
        referrerUserId: referrer._id,
        referredUserId: user._id,
        referralCode: cleanReferralCode,
        status: "signed_up",
        friendDiscountAmount: REFERRAL_DISCOUNT_AMOUNT,
        referrerRewardAmount: REFERRAL_DISCOUNT_AMOUNT,
      });
    }

    const token = signToken(user);

    if (!isMobileClient(req)) {
      res.cookie("token", token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: process.env.NODE_ENV === "production" ? "none" : "lax",
        maxAge: 7 * 24 * 60 * 60 * 1000,
      });
    }

    return res.status(201).json({
      message:
        newRole === "provider" && status === "pending"
          ? "Provider account created successfully. Awaiting admin approval."
          : "Account created successfully.",
      token,
      user: buildAuthUser(user),
    });
  } catch (e) {
    console.error("signup error:", e);
    return res.status(500).json({
      message: e.message || "Server error",
    });
  }
}

export async function login(req, res) {
  try {
    const { email, password } = req.body || {};

    if (!email || !password) {
      return res.status(400).json({
        message: "Email and password are required",
      });
    }

    const user = await User.findOne({
      email: String(email).toLowerCase().trim(),
    });

    if (!user) {
      return res.status(401).json({
        message: "Invalid credentials",
      });
    }

    const ok = await user.comparePassword(password);
    if (!ok) {
      return res.status(401).json({
        message: "Invalid credentials",
      });
    }

    if (user.status === "inactive") {
      return res.status(403).json({
        message: "Account is inactive",
      });
    }

    if (user.role === "provider" && user.status === "pending") {
      return res.status(403).json({
        message: "Provider account is pending admin approval.",
      });
    }

    if (user.role === "provider" && user.status === "rejected") {
      return res.status(403).json({
        message: "Provider account has been rejected.",
      });
    }

    if (user.role === "provider" && user.status === "suspended") {
      return res.status(403).json({
        message: "Provider account is suspended.",
      });
    }

    const clientSource = isMobileClient(req) ? "android" : "web";
    user.lastLoginFrom = clientSource;
    await user.save();

    const token = signToken(user);

    if (!isMobileClient(req)) {
      res.cookie("token", token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: process.env.NODE_ENV === "production" ? "none" : "lax",
        maxAge: 7 * 24 * 60 * 60 * 1000,
      });
    }

    return res.json({
      message: "Login successful",
      token,
      user: buildAuthUser(user),
    });
  } catch (e) {
    console.error("login error:", e);
    return res.status(500).json({
      message: e.message || "Server error",
    });
  }
}

export async function me(req, res) {
  try {
    const userId = req.user?.id || req.user?._id;

    if (!userId) {
      return res.status(401).json({
        message: "Unauthorized",
      });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({
        message: "User not found",
      });
    }

    return res.json({
      user: buildAuthUser(user),
    });
  } catch (e) {
    console.error("me error:", e);
    return res.status(500).json({
      message: e.message || "Server error",
    });
  }
}

export async function logout(req, res) {
  res.clearCookie("token", {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: process.env.NODE_ENV === "production" ? "none" : "lax",
  });

  return res.json({
    message: "Logged out",
  });
}

export async function forgotPassword(req, res) {
  try {
    const email = String(req.body.email || "").trim().toLowerCase();

    if (!email) {
      return res.status(400).json({ message: "Email is required" });
    }

    const user = await User.findOne({ email });

    if (!user) {
      return res.status(200).json({
        message: "If that email exists, a reset link has been sent.",
      });
    }

    const resetToken = jwt.sign(
      { id: user._id, email: user.email, type: "password_reset" },
      process.env.JWT_SECRET,
      { expiresIn: "30m" }
    );

    const resetLink = `${process.env.FRONTEND_RESET_URL}?token=${resetToken}`;

    await sendResetEmail(user.email, resetLink);

    return res.status(200).json({
      message: "Reset link sent to your email.",
    });
  } catch (error) {
    console.error("forgotPassword error:", error);
    return res.status(500).json({
      message: "Failed to send reset link",
    });
  }
}

export async function resetPassword(req, res) {
  try {
    const { token, newPassword } = req.body || {};

    if (!token || !newPassword) {
      return res.status(400).json({
        message: "Token and new password are required",
      });
    }

    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    if (decoded.type !== "password_reset") {
      return res.status(400).json({
        message: "Invalid reset token",
      });
    }

    const user = await User.findById(decoded.id);
    if (!user) {
      return res.status(404).json({
        message: "User not found",
      });
    }

    user.password = await bcrypt.hash(newPassword, 10);
    await user.save();

    return res.status(200).json({
      message: "Password reset successful",
    });
  } catch (error) {
    console.error("resetPassword error:", error);
    return res.status(400).json({
      message: "Reset link is invalid or expired",
    });
  }
}