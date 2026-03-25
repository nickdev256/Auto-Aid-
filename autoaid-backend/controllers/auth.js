import User from "../models/User.js";
import Settings from "../models/Settings.js";
import { signToken } from "../utils/jwt.js";

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
    verificationStatus: user.verificationStatus || "not_verified",
    registeredFrom: user.registeredFrom || "web",
    lastLoginFrom: user.lastLoginFrom || "web",
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
    const { name, email, password, role, phone } = req.body || {};

    if (!name || !email || !password) {
      return res.status(400).json({
        message: "Name, email, password are required",
      });
    }

    const cleanEmail = String(email).toLowerCase().trim();
    const cleanName = String(name).trim();
    const cleanPhone = String(phone || "").trim();

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

    let status = "active";

    if (newRole === "provider") {
      status = settings.autoApproveProviders ? "approved" : "pending";
    }

    const clientSource = isMobileClient(req) ? "android" : "web";

    const user = await new User({
      name: cleanName,
      email: cleanEmail,
      password,
      phone: cleanPhone,
      role: newRole,
      status,
      verificationStatus: "not_verified",
      registeredFrom: clientSource,
      lastLoginFrom: clientSource,
    }).save();

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