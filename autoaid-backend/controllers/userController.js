import User from "../models/User.js";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";

export const registerUser = async (req, res) => {
    const { name, email, password, role, businessType, subscription } = req.body;
    try {
        const existing = await User.findOne({ email });
        if (existing) return res.status(400).json({ message: "Email already exists" });

        const hashedPassword = await bcrypt.hash(password, 10);

        const user = await User.create({ name, email, password: hashedPassword, role, businessType, subscription });

        res.status(201).json({ message: "User registered", user });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
};

export const loginUser = async (req, res) => {
  const { email, password } = req.body;

  try {
    const user = await User.findOne({ email });
    if (!user) return res.status(400).json({ message: "Invalid credentials" });

    const match = await bcrypt.compare(password, user.password);
    if (!match) return res.status(400).json({ message: "Invalid credentials" });

    const token = jwt.sign(
      { id: user._id, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn: "1d" }
    );

    // ✅ Set HttpOnly cookie (Option A)
    res.cookie("token", token, {
      httpOnly: true,
      sameSite: "lax",     // good for localhost
      secure: false,       // true in production https
      maxAge: 24 * 60 * 60 * 1000,
    });

    // ✅ Don’t return token to JS if you want strict cookie auth
    res.json({
      message: "Logged in",
      user: {
        _id: user._id,
        name: user.name,
        email: user.email,
        role: user.role,
        businessType: user.businessType,
        subscription: user.subscription,
      },
    });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
};

export const getUsers = async (req, res) => {
    try {
        const users = await User.find();
        res.json(users);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
};
// backend/controllers/pesapalController.js
export const logoutUser = async (req, res) => {
  res.clearCookie("token", {
    httpOnly: true,
    sameSite: "lax",
    secure: false,
  });
  res.json({ message: "Logged out" });
};

export async function initiatePesapalPayment(req, res) {
  try {
    const { providerId, planId, phone, network } = req.body;

    if (!providerId || !planId || !phone || !network) {
      return res.status(400).send("Missing required fields");
    }

    // Call Pesapal OR your Mobile Money aggregator here
    console.log("Starting MoMo payment:", req.body);

    // simulate
    res.json({
      success: true,
      message: "Payment initiated. Prompt sent to phone.",
    });

  } catch (err) {
    console.error("Pesapal payment failed:", err);
    res.status(500).send("Payment failed");
  }
}

