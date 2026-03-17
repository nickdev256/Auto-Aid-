import User from "../models/User.js";
import { signToken } from "../utils/jwt.js";

function isMobileClient(req) {
  // Android can send X-Client: mobile
  return (req.headers["x-client"] || "").toString().toLowerCase() === "mobile";
}

export async function signup(req, res) {
  try {
    const { name, email, password, role } = req.body || {};

    if (!name || !email || !password) {
      return res.status(400).json({ message: "Name, email, password are required" });
    }

    const cleanEmail = email.toLowerCase().trim();

    const exists = await User.findOne({ email: cleanEmail });
    if (exists) return res.status(409).json({ message: "Email already exists" });

    // If provider signs up, often you want pending approval
    const newRole = role && ["user", "provider"].includes(role) ? role : "user";
    const status = newRole === "provider" ? "pending" : "active";

    const user = await new User({
      name,
      email: cleanEmail,
      password, // hashed by your model pre-save
      role: newRole,
      status,
    }).save();

    const token = signToken(user);

    // optional cookie for web
    if (!isMobileClient(req)) {
      res.cookie("token", token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: process.env.NODE_ENV === "production" ? "none" : "lax",
        maxAge: 7 * 24 * 60 * 60 * 1000,
      });
    }

    return res.status(201).json({
      token,
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: user.role,
        status: user.status,
      },
    });
  } catch (e) {
    return res.status(500).json({ message: e.message || "Server error" });
  }
}

export async function login(req, res) {
  try {
    const { email, password } = req.body || {};

    if (!email || !password) {
      return res.status(400).json({ message: "Email and password are required" });
    }

    const user = await User.findOne({ email: email.toLowerCase().trim() });
    if (!user) return res.status(401).json({ message: "Invalid credentials" });

    const ok = await user.comparePassword(password);
    if (!ok) return res.status(401).json({ message: "Invalid credentials" });

    if (user.status === "inactive") {
      return res.status(403).json({ message: "Account is inactive" });
    }

    const token = signToken(user);

    // optional cookie for web
    if (!isMobileClient(req)) {
      res.cookie("token", token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: process.env.NODE_ENV === "production" ? "none" : "lax",
        maxAge: 7 * 24 * 60 * 60 * 1000,
      });
    }

    return res.json({
      token,
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: user.role,
        status: user.status,
      },
    });
  } catch (e) {
    return res.status(500).json({ message: e.message || "Server error" });
  }
}

export async function logout(req, res) {
  // Web uses cookie, so clear it
  res.clearCookie("token", {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: process.env.NODE_ENV === "production" ? "none" : "lax",
  });
  return res.json({ message: "Logged out" });
}