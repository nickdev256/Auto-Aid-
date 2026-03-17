import jwt from "jsonwebtoken";
import User from "../models/User.js";
import "dotenv/config";

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  throw new Error("❌ JWT_SECRET is missing in environment (.env)");
}

export const protect = async (req, res, next) => {
  try {
    // ✅ IMPORTANT: allow CORS preflight requests through
    if (req.method === "OPTIONS") return next();

    // ✅ Android sends Bearer token
    const authHeader = req.headers.authorization || "";
    const bearerToken = authHeader.startsWith("Bearer ")
      ? authHeader.slice(7)
      : null;

    // ✅ Web uses cookie token
    const cookieToken = req.cookies?.token;

    const token = bearerToken || cookieToken;

    if (!token) {
      return res.status(401).json({ message: "Not authenticated (missing token)" });
    }

    const decoded = jwt.verify(token, JWT_SECRET);

    const user = await User.findById(decoded.id).select("-password");
    if (!user) {
      return res.status(401).json({ message: "User not found" });
    }

    req.user = user;
    next();
  } catch (err) {
    console.error("Auth error:", err.message);

    if (err.name === "TokenExpiredError") {
      return res.status(401).json({ message: "Token expired" });
    }
    if (err.name === "JsonWebTokenError") {
      return res.status(401).json({ message: `Invalid token: ${err.message}` });
    }

    return res.status(401).json({ message: "Authentication failed" });
  }
};

export const authorize = (...roles) => {
  return (req, res, next) => {
    // ✅ allow CORS preflight requests through
    if (req.method === "OPTIONS") return next();

    if (!req.user || !req.user.role) {
      return res.status(403).json({ message: "Access denied" });
    }

    if (!roles.includes(req.user.role)) {
      return res.status(403).json({ message: "Access denied" });
    }

    next();
  };
};