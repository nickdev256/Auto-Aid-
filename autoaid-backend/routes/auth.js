import express from "express";
import jwt from "jsonwebtoken";
import User from "../models/User.js";
import OTP from "../models/OTP.js";
import sendEmailOTP from "../utils/sendEmailOTP.js";

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

/* ✅ NEW: detect platform (android or web) using X-Client header */
const getClient = (req) => {
  const x = (req.headers["x-client"] || "").toString().toLowerCase();
  return x === "android" ? "android" : "web";
};

const getTokenFromReq = (req) => {
  const authHeader = req.headers.authorization || "";
  const bearer = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;
  return bearer || req.cookies?.token || null;
};

const signToken = (user) =>
  jwt.sign({ id: user._id, role: user.role }, JWT_SECRET, { expiresIn: "1d" });

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
    } = req.body;

    const cleanEmail = normalizeEmail(email);

    if (!name) return res.status(400).json({ message: "Name is required" });
    if (!cleanEmail) return res.status(400).json({ message: "Email is required" });
    if (!password) return res.status(400).json({ message: "Password is required" });

    if (role === "provider" && !businessType) {
      return res.status(400).json({ message: "Business type is required for providers" });
    }

    const exists = await User.findOne({ email: cleanEmail });
    if (exists) return res.status(400).json({ message: "Email already exists" });

    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    await OTP.findOneAndUpdate(
      { email: cleanEmail },
      {
        email: cleanEmail,
        otp,
        expiresAt: Date.now() + 5 * 60 * 1000,
        formData: {
          name,
          email: cleanEmail,
          phone,
          password, // stored temporarily, User model hashes on save
          role,
          businessName,
          businessType,
          servicesOffered,
          subscriptionPlan,
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

    const cleanEmail = normalizeEmail(req.body.email);
    if (!cleanEmail) return res.status(400).json({ message: "Email is required" });

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
   STEP 2 → VERIFY OTP (Create User + Return Token + Cookie)
====================================================== */
router.post("/verify-otp", async (req, res) => {
  try {
    console.log("✅ /verify-otp body:", req.body);

    const client = getClient(req); // ✅ android | web
    const cleanEmail = normalizeEmail(req.body.email);
    const otp = (req.body.otp || "").trim();

    if (!cleanEmail || !otp) {
      return res.status(400).json({ message: "Email and OTP are required" });
    }

    const record = await OTP.findOne({ email: cleanEmail });
    if (!record) return res.status(400).json({ message: "OTP not found" });
    if (record.otp !== otp) return res.status(400).json({ message: "Incorrect OTP" });
    if (record.expiresAt < Date.now()) return res.status(400).json({ message: "OTP expired" });

    const data = record.formData;
    if (!data?.password) {
      return res.status(400).json({ message: "Signup data missing. Please signup again." });
    }

    const already = await User.findOne({ email: cleanEmail });
    if (already) {
      await OTP.deleteOne({ email: cleanEmail });

      // ✅ update lastLoginFrom for existing user
      already.lastLoginFrom = client;
      await already.save();

      const token = signToken(already);
      res.cookie("token", token, cookieOptions);

      const safe = already.toObject();
      delete safe.password;

      return res.json({ message: "Account already verified", token, user: safe });
    }

    const user = await User.create({
      ...data,
      email: cleanEmail,
      role: data.role || "user",
      status: data.role === "provider" ? "pending" : "approved",

      // ✅ NEW: platform tracking
      registeredFrom: client,
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

    const safeUser = user.toObject();
    delete safeUser.password;

    return res.json({
      message: "Account verified successfully",
      token,
      user: safeUser,
    });
  } catch (err) {
    console.error("❌ Verification error:", err);
    return res.status(500).json({ message: err.message || "Verification failed" });
  }
});

/* =====================================================
   LOGIN (RETURN TOKEN + COOKIE) + updates lastLoginFrom
====================================================== */
router.post("/login", async (req, res) => {
  try {
    console.log("✅ /login body:", req.body);

    const client = getClient(req); // ✅ android | web
    const cleanEmail = normalizeEmail(req.body.email);
    const password = req.body.password;

    if (!cleanEmail || !password) {
      return res.status(400).json({ message: "Email and password are required" });
    }

    const user = await User.findOne({ email: cleanEmail });
    if (!user) return res.status(400).json({ message: "Invalid credentials" });

    const isMatch = await user.comparePassword(password);
    if (!isMatch) return res.status(400).json({ message: "Invalid credentials" });

    // ✅ update login platform
    user.lastLoginFrom = client;
    await user.save();

    const token = signToken(user);
    res.cookie("token", token, cookieOptions);

    const safe = user.toObject();
    delete safe.password;

    return res.json({ token, user: safe });
  } catch (err) {
    console.error("❌ Login error:", err);
    return res.status(500).json({ message: err.message || "Server error" });
  }
});

/* =====================================================
   ME (COOKIE OR BEARER)
====================================================== */
router.get("/me", async (req, res) => {
  try {
    const token = getTokenFromReq(req);
    if (!token) return res.status(401).json({ message: "Not logged in" });

    const decoded = jwt.verify(token, JWT_SECRET);
    const user = await User.findById(decoded.id).select("-password");
    if (!user) return res.status(401).json({ message: "User not found" });

    return res.json({ user });
  } catch (err) {
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

export default router;