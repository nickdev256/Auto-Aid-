import jwt from "jsonwebtoken";
import User from "../models/User.js";
import "dotenv/config";

const JWT_SECRET = process.env.JWT_SECRET;

if (!JWT_SECRET) {
  throw new Error("JWT_SECRET is missing in environment (.env)");
}

function extractToken(req) {
  const authHeader = req.headers.authorization || "";
  const bearerToken = authHeader.startsWith("Bearer ")
    ? authHeader.slice(7)
    : null;

  const cookieToken = req.cookies?.token || null;

  return bearerToken || cookieToken || null;
}

function normalizeRole(role) {
  return String(role || "").trim().toLowerCase();
}

export const protect = async (req, res, next) => {
  try {
    if (req.method === "OPTIONS") return next();

    const token = extractToken(req);

    if (!token) {
      return res.status(401).json({
        message: "Not authenticated (missing token)",
      });
    }

    const decoded = jwt.verify(token, JWT_SECRET);

    if (!decoded?.id) {
      return res.status(401).json({
        message: "Invalid token payload",
      });
    }

    const user = await User.findById(decoded.id).select("-password");

    if (!user) {
      return res.status(401).json({
        message: "User not found",
      });
    }

    req.user = user;
    next();
  } catch (err) {
    console.error("Auth error:", err.message);

    if (err.name === "TokenExpiredError") {
      return res.status(401).json({
        message: "Token expired",
      });
    }

    if (err.name === "JsonWebTokenError") {
      return res.status(401).json({
        message: "Invalid token",
      });
    }

    return res.status(401).json({
      message: "Authentication failed",
    });
  }
};

export const authorize = (...roles) => {
  const normalizedRoles = roles.map((r) => normalizeRole(r));

  return (req, res, next) => {
    if (req.method === "OPTIONS") return next();

    if (!req.user || !req.user.role) {
      return res.status(403).json({
        message: "Access denied",
      });
    }

    const userRole = normalizeRole(req.user.role);

    if (!normalizedRoles.includes(userRole)) {
      return res.status(403).json({
        message: "Access denied",
      });
    }

    next();
  };
};