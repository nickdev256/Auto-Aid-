import express from "express";
import jwt from "jsonwebtoken";
import bcrypt from "bcryptjs";
import User from "../models/User.js";
import OTP from "../models/OTP.js";
import sendEmailOTP, { sendResetEmail } from "../utils/sendEmailOTP.js";

const router = express.Router();

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  throw new Error("JWT_SECRET missing in environment");
}

const cookieOptions = {
  httpOnly: true,
  sameSite: process.env.NODE_ENV === "production" ? "none" : "lax",
  secure: process.env.NODE_ENV === "production",
  maxAge: 24 * 60 * 60 * 1000,
};

const normalizeEmail = (email) => (email || "").trim().toLowerCase();

const getClient = (req) => {
  const x = (req.headers["x-client"] || "").toString().toLowerCase();
  return x === "android" || x === "mobile" ? "android" : "web";
};

const getTokenFromReq = (req) => {
  const authHeader = req.headers.authorization || "";
  const bearer = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;
  return bearer || req.cookies?.token || null;
};

const signToken = (user) =>
  jwt.sign({ id: user._id, role: user.role }, JWT_SECRET, { expiresIn: "1d" });

function buildUserResponse(userDoc) {
  const user =
    typeof userDoc?.getDecrypted === "function"
      ? userDoc.getDecrypted()
      : userDoc?.toObject?.() || userDoc;

  return {
    id: user?._id || user?.id || "",
    _id: user?._id || user?.id || "",
    name: user?.name || "",
    email: user?.email || "",
    phone: user?.phone || "",
    role: user?.role || "user",
    status: user?.status || "active",
    verificationStatus: user?.verificationStatus || "not_verified",
  };
}

/* =====================================================
   STEP 1 → SIGNUP (Generate OTP + Save Form Data)
====================================================== */
router.post("/signup", async (req, res) => {
  try {
    console.log("✅ /signup body:", req.body);

    const {
      name,
      email,
      phone,
      password,
      role,
      businessName = "",
      businessType = "",
      servicesOffered = [],
      subscriptionPlan = null,
    } = req.body || {};

    const client = getClient(req);
    const cleanEmail = normalizeEmail(email);

    if (!name?.trim()) {
      return res.status(400).json({ message: "Name is required" });
    }
    if (!cleanEmail) {
      return res.status(400).json({ message: "Email is required" });
    }
    if (!password) {
      return res.status(400).json({ message: "Password is required" });
    }

    if (role === "provider" && !businessType) {
      return res
        .status(400)
        .json({ message: "Business type is required for providers" });
    }

    const exists = await User.findOne({ email: cleanEmail });
    if (exists) {
      return res.status(400).json({ message: "Email already exists" });
    }

    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    await OTP.findOneAndUpdate(
      { email: cleanEmail },
      {
        email: cleanEmail,
        otp,
        expiresAt: Date.now() + 5 * 60 * 1000,
        formData: {
          name: name.trim(),
          email: cleanEmail,
          phone: phone || "",
          password,
          role: role || "user",
          businessName,
          businessType,
          servicesOffered,
          subscriptionPlan,
          registeredFrom: client,
          lastLoginFrom: client,
        },
      },
      { upsert: true, new: true }
    );

    try {
      await sendEmailOTP(cleanEmail, otp);
    } catch (e) {
      console.warn("⚠️ sendEmailOTP failed:", e?.message);
    }

    return res.json({
      message: "Signup initiated. OTP sent.",
      email: cleanEmail,
    });
  } catch (err) {
    console.error("❌ Signup error:", err);
    return res.status(500).json({ message: err.message || "Signup failed" });
  }
});

/* =====================================================
   RESEND OTP
====================================================== */
router.post("/resend-otp", async (req, res) => {
  try {
    console.log("✅ /resend-otp body:", req.body);

    const cleanEmail = normalizeEmail(req.body?.email);
    if (!cleanEmail) {
      return res.status(400).json({ message: "Email is required" });
    }

    const record = await OTP.findOne({ email: cleanEmail });
    if (!record) {
      return res
        .status(400)
        .json({ message: "OTP record not found. Please signup again." });
    }

    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    record.otp = otp;
    record.expiresAt = Date.now() + 5 * 60 * 1000;
    await record.save();

    try {
      await sendEmailOTP(cleanEmail, otp);
    } catch (e) {
      console.warn("⚠️ resend sendEmailOTP failed:", e?.message);
    }

    return res.json({ message: "New OTP sent." });
  } catch (err) {
    console.error("❌ Resend OTP error:", err);
    return res.status(500).json({ message: err.message || "Failed to resend OTP" });
  }
});

/* =====================================================
   STEP 2 → VERIFY OTP
====================================================== */
router.post("/verify-otp", async (req, res) => {
  try {
    console.log("✅ /verify-otp body:", req.body);

    const client = getClient(req);
    const cleanEmail = normalizeEmail(req.body?.email);
    const otp = (req.body?.otp || "").trim();

    if (!cleanEmail || !otp) {
      return res.status(400).json({ message: "Email and OTP are required" });
    }

    const record = await OTP.findOne({ email: cleanEmail });
    if (!record) {
      return res.status(400).json({ message: "OTP not found" });
    }
    if (record.otp !== otp) {
      return res.status(400).json({ message: "Incorrect OTP" });
    }
    if (record.expiresAt < Date.now()) {
      return res.status(400).json({ message: "OTP expired" });
    }

    const data = record.formData;
    if (!data?.password) {
      return res
        .status(400)
        .json({ message: "Signup data missing. Please signup again." });
    }

    const already = await User.findOne({ email: cleanEmail });
    if (already) {
      await OTP.deleteOne({ email: cleanEmail });

      already.lastLoginFrom = client;
      await already.save();

      const token = signToken(already);
      res.cookie("token", token, cookieOptions);

      return res.json({
        message: "Account already verified",
        token,
        user: buildUserResponse(already),
      });
    }

    const user = await User.create({
      ...data,
      email: cleanEmail,
      phone: data.phone || "",
      role: data.role || "user",
      status: data.role === "provider" ? "pending" : "approved",
      verificationStatus: "not_verified",
      registeredFrom: data.registeredFrom || client,
      lastLoginFrom: client,
      subscription: {
        plan: data.subscriptionPlan || null,
        active: false,
        startDate: null,
        expiryDate: null,
        paymentMethod: null,
        price: 0,
      },
    });

    await OTP.deleteOne({ email: cleanEmail });

    const token = signToken(user);
    res.cookie("token", token, cookieOptions);

    return res.json({
      message: "Account verified successfully",
      token,
      user: buildUserResponse(user),
    });
  } catch (err) {
    console.error("❌ Verification error:", err);
    return res.status(500).json({ message: err.message || "Verification failed" });
  }
});

/* =====================================================
   LOGIN
====================================================== */
router.post("/login", async (req, res) => {
  try {
    console.log("✅ /login body:", req.body);

    const client = getClient(req);
    const cleanEmail = normalizeEmail(req.body?.email);
    const password = req.body?.password;

    if (!cleanEmail || !password) {
      return res.status(400).json({ message: "Email and password are required" });
    }

    const user = await User.findOne({ email: cleanEmail });
    if (!user) {
      return res.status(400).json({ message: "Invalid credentials" });
    }

    const isMatch = await user.comparePassword(password);
    if (!isMatch) {
      return res.status(400).json({ message: "Invalid credentials" });
    }

    if (user.status === "inactive") {
      return res.status(403).json({ message: "Account is inactive" });
    }

    user.lastLoginFrom = client;
    await user.save();

    const token = signToken(user);
    res.cookie("token", token, cookieOptions);

    return res.json({
      message: "Login successful",
      token,
      user: buildUserResponse(user),
    });
  } catch (err) {
    console.error("❌ Login error:", err);
    return res.status(500).json({ message: err.message || "Server error" });
  }
});

/* =====================================================
   ME
====================================================== */
router.get("/me", async (req, res) => {
  try {
    const token = getTokenFromReq(req);
    if (!token) {
      return res.status(401).json({ message: "Not logged in" });
    }

    const decoded = jwt.verify(token, JWT_SECRET);
    const user = await User.findById(decoded.id);

    if (!user) {
      return res.status(401).json({ message: "User not found" });
    }

    return res.json({
      user: buildUserResponse(user),
    });
  } catch (err) {
    console.error("❌ /me error:", err);
    return res.status(401).json({ message: "Invalid session" });
  }
});

/* =====================================================
   LOGOUT
====================================================== */
router.post("/logout", (req, res) => {
  res.clearCookie("token", {
    httpOnly: true,
    sameSite: process.env.NODE_ENV === "production" ? "none" : "lax",
    secure: process.env.NODE_ENV === "production",
  });

  return res.json({ message: "Logged out" });
});

/* =====================================================
   FORGOT PASSWORD
====================================================== */
router.post("/forgot-password", async (req, res) => {
  try {
    const email = normalizeEmail(req.body?.email);

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
      JWT_SECRET,
      { expiresIn: "30m" }
    );

    const resetLink = `${process.env.FRONTEND_RESET_URL}?token=${resetToken}`;

    await sendResetEmail(user.email, resetLink);

    return res.status(200).json({
      message: "Reset link sent to your email.",
    });
  } catch (error) {
    console.error("forgotPassword error:", error);
    return res.status(500).json({ message: "Failed to send reset link" });
  }
});

/* =====================================================
   RESET PASSWORD
====================================================== */
router.post("/reset-password", async (req, res) => {
  try {
    const { token, newPassword } = req.body || {};

    if (!token || !newPassword) {
      return res.status(400).json({
        message: "Token and new password are required",
      });
    }

    const decoded = jwt.verify(token, JWT_SECRET);

    if (decoded.type !== "password_reset") {
      return res.status(400).json({ message: "Invalid reset token" });
    }

    const user = await User.findById(decoded.id);
    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    user.password = await bcrypt.hash(newPassword, 10);
    await user.save();

    return res.status(200).json({
      message: "Password reset successful",
    });
  } catch (error) {
    console.error("resetPassword error:", error);
    return res.status(400).json({ message: "Reset link is invalid or expired" });
  }
});

export default router;