import jwt from "jsonwebtoken";
import User from "../models/User.js";
import "dotenv/config";

const JWT_SECRET = process.env.JWT_SECRET;

if (!JWT_SECRET) {
  throw new Error("JWT_SECRET is missing in environment (.env)");
}

/**
 * Extract token from:
 * - Authorization header (Bearer token)
 * - Cookies (fallback)
 */
function extractToken(req) {
  const authHeader = req.headers.authorization;

  if (authHeader && authHeader.startsWith("Bearer ")) {
    return authHeader.split(" ")[1];
  }

  return req.cookies?.token || null;
}

/**
 * Normalize roles for safe comparison
 */
function normalizeRole(role) {
  return String(role || "").trim().toLowerCase();
}

/**
 * AUTH MIDDLEWARE (PROTECTED ROUTES)
 */
export const protect = async (req, res, next) => {
  try {
    if (req.method === "OPTIONS") return next();

    const token = extractToken(req);

    if (!token) {
      return res.status(401).json({
        success: false,
        message: "Authentication required (no token provided)",
      });
    }

    let decoded;

    try {
      decoded = jwt.verify(token, JWT_SECRET);
    } catch (err) {
      // 🔥 Handle JWT-specific errors cleanly
      if (err.name === "TokenExpiredError") {
        return res.status(401).json({
          success: false,
          message: "Session expired. Please login again.",
        });
      }

      if (err.name === "JsonWebTokenError") {
        return res.status(401).json({
          success: false,
          message: "Invalid authentication token",
        });
      }

      return res.status(401).json({
        success: false,
        message: "Authentication failed",
      });
    }

    if (!decoded?.id) {
      return res.status(401).json({
        success: false,
        message: "Invalid token structure",
      });
    }

    const user = await User.findById(decoded.id).select("-password");

    if (!user) {
      return res.status(401).json({
        success: false,
        message: "User not found or deleted",
      });
    }

    req.user = user;
    next();
  } catch (err) {
    console.error("AUTH MIDDLEWARE ERROR:", err);

    return res.status(500).json({
      success: false,
      message: "Internal authentication error",
    });
  }
};

/**
 * ROLE-BASED ACCESS CONTROL
 */
export const authorize = (...roles) => {
  const allowedRoles = roles.map(normalizeRole);

  return (req, res, next) => {
    if (req.method === "OPTIONS") return next();

    if (!req.user || !req.user.role) {
      return res.status(403).json({
        success: false,
        message: "Access denied (no role found)",
      });
    }

    const userRole = normalizeRole(req.user.role);

    if (!allowedRoles.includes(userRole)) {
      return res.status(403).json({
        success: false,
        message: "You do not have permission to access this resource",
      });
    }

    next();
  };
};