// server.js
import dotenv from "dotenv";
import path from "path";
import fs from "fs";
import compression from "compression";
import { fileURLToPath, pathToFileURL } from "url";
import navigationRoutes from "./routes/navigation.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

dotenv.config({ path: path.join(__dirname, ".env") });

import express from "express";
import cors from "cors";
import mongoose from "mongoose";
import http from "http";
import { Server } from "socket.io";
import cookieParser from "cookie-parser";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import jwt from "jsonwebtoken";
import cookie from "cookie";

// ROUTES
import chatRoutes from "./routes/chat.js";
import authRoutes from "./routes/auth.js";
import adminRoutes from "./routes/admin.js";
import providersRoutes from "./routes/providers.js";
import fuelRoutes from "./routes/fuel.js";
import towingRoutes from "./routes/towing.js";
import ambulanceRoutes from "./routes/ambulance.js";
import garageRoutes from "./routes/garage.js";
import paymentsRoutes from "./routes/payments.js";
import requestsRoutes from "./routes/requests.js";
import verificationRoutes from "./routes/verification.js";
import settingsRoutes from "./routes/settingsRoutes.js";
import aiRoutes from "./routes/aiAssist.js";
import userWalletRoutes from "./routes/userWallet.js";
import extraChargesRoutes from "./routes/extraCharges.js";
import referralRoutes from "./routes/referralRoutes.js";
import subscriptionRoutes from "./routes/subscriptions.js";
import userVerificationRoutes from "./routes/userVerification.js";
import providerVerificationRoutes from "./routes/providerVerification.js";

// MODELS
import User from "./models/User.js";
import ChatMessage from "./models/Chat.js";
import Request from "./models/Request.js";
import Settings from "./models/Settings.js";

const app = express();
const server = http.createServer(app);

console.log("ENV FILE LOADED. MONGO_URI exists =", !!process.env.MONGO_URI);

const IS_DEV = process.env.NODE_ENV !== "production";

/* =================================================
   SETTINGS CACHE
================================================= */
let cachedSettings = null;
let lastSettingsFetch = 0;
const SETTINGS_CACHE_TTL_MS = 10_000;

async function getCachedSettings(force = false) {
  if (!isDbReady()) return cachedSettings;

  const now = Date.now();
  const shouldRefresh =
    force ||
    !cachedSettings ||
    now - lastSettingsFetch > SETTINGS_CACHE_TTL_MS;

  if (shouldRefresh) {
    cachedSettings = await Settings.findOne().lean();
    lastSettingsFetch = now;
  }

  return cachedSettings;
}

function invalidateSettingsCache() {
  cachedSettings = null;
  lastSettingsFetch = 0;
}

/* =================================================
   ENSURE UPLOAD DIRECTORIES EXIST
================================================= */
const uploadsRoot = path.join(__dirname, "uploads");
const providerVerificationUploads = path.join(
  __dirname,
  "uploads",
  "provider-verification"
);

if (!fs.existsSync(uploadsRoot)) {
  fs.mkdirSync(uploadsRoot, { recursive: true });
}

if (!fs.existsSync(providerVerificationUploads)) {
  fs.mkdirSync(providerVerificationUploads, { recursive: true });
}

/* =================================================
   OPTIONAL ROUTE LOADER
================================================= */
async function loadOptionalRoute(relativePath, label) {
  try {
    const absolutePath = path.join(__dirname, relativePath);

    if (!fs.existsSync(absolutePath)) {
      console.warn(`Optional route missing: ${relativePath} (${label})`);
      return null;
    }

    const mod = await import(pathToFileURL(absolutePath).href);
    const router = mod?.default || null;

    if (!router) {
      console.warn(`Optional route has no default export: ${relativePath}`);
      return null;
    }

    console.log(`Optional route loaded: ${relativePath}`);
    return router;
  } catch (err) {
    console.error(`Failed to load optional route ${relativePath}:`, err.message);
    return null;
  }
}

/* =================================================
   HELPERS
================================================= */
function isDbReady() {
  return mongoose.connection.readyState === 1;
}

/* =================================================
   CORS HELPERS
================================================= */
const FRONTEND_ALLOWED = [
  "http://localhost:5173",
  "http://127.0.0.1:5173",
  "http://localhost:5174",
  "http://127.0.0.1:5174",
];

const LAN_REGEX = /^http:\/\/192\.168\.\d{1,3}\.\d{1,3}(:\d+)?$/;
const EMULATOR_REGEX = /^http:\/\/10\.0\.2\.2(:\d+)?$/;
const LOCALHOST_ANY_PORT = /^http:\/\/localhost:\d+$/;
const LOOPBACK_ANY_PORT = /^http:\/\/127\.0\.0\.1:\d+$/;

function isAllowedOrigin(origin) {
  if (!origin) return true;
  if (FRONTEND_ALLOWED.includes(origin)) return true;
  if (LAN_REGEX.test(origin)) return true;
  if (EMULATOR_REGEX.test(origin)) return true;
  if (LOCALHOST_ANY_PORT.test(origin)) return true;
  if (LOOPBACK_ANY_PORT.test(origin)) return true;
  return false;
}

function corsOrigin(origin, callback) {
  if (isAllowedOrigin(origin)) {
    return callback(null, true);
  }
  console.warn("CORS blocked origin:", origin);
  return callback(new Error(`CORS blocked for origin: ${origin}`));
}

const corsOptions = {
  origin: corsOrigin,
  credentials: true,
  methods: ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
  allowedHeaders: [
    "Content-Type",
    "Authorization",
    "X-Client",
    "x-client",
    "Accept",
    "Origin",
  ],
  optionsSuccessStatus: 204,
};

/* =================================================
   VERY IMPORTANT: CORS FIRST
================================================= */
app.use((req, res, next) => {
  const origin = req.headers.origin;

  if (isAllowedOrigin(origin)) {
    res.header("Access-Control-Allow-Origin", origin || "*");
  }

  res.header("Vary", "Origin");
  res.header("Access-Control-Allow-Credentials", "true");
  res.header(
    "Access-Control-Allow-Headers",
    "Content-Type, Authorization, X-Client, x-client, Accept, Origin"
  );
  res.header(
    "Access-Control-Allow-Methods",
    "GET, POST, PUT, PATCH, DELETE, OPTIONS"
  );

  if (req.method === "OPTIONS") {
    if (IS_DEV) {
      console.log(
        "PREFLIGHT:",
        req.method,
        req.originalUrl,
        "Origin:",
        origin || "none"
      );
    }
    return res.sendStatus(204);
  }

  next();
});

app.use(cors(corsOptions));

/* =================================================
   BASIC RESPONSE HEADERS + COMPRESSION
================================================= */
app.disable("x-powered-by");

app.use((req, res, next) => {
  res.setHeader("X-Powered-By", "AutoAid");
  res.setHeader("Cache-Control", "no-store");
  next();
});

app.use(compression());

/* =================================================
   SECURITY
================================================= */
app.use(
  helmet({
    crossOriginResourcePolicy: false,
  })
);

app.use(cookieParser());

/* =================================================
   RATE LIMITS
================================================= */
const skipLocalInDev = (req) => {
  const ip = req.ip || req.socket?.remoteAddress || "";
  return (
    process.env.NODE_ENV !== "production" &&
    (ip.includes("127.0.0.1") ||
      ip.includes("::1") ||
      ip.includes("localhost") ||
      ip.includes("::ffff:127.0.0.1"))
  );
};

const generalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 1000,
  skip: skipLocalInDev,
  standardHeaders: true,
  legacyHeaders: false,
  message: { message: "Too many requests, please try again later." },
});

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 20,
  skip: skipLocalInDev,
  standardHeaders: true,
  legacyHeaders: false,
  message: { message: "Too many auth attempts, please try again later." },
});

const adminLimiter = rateLimit({
  windowMs: 5 * 60 * 1000,
  max: 300,
  skip: skipLocalInDev,
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req, res) => {
    console.warn("Rate limit hit:", req.method, req.originalUrl, "IP:", req.ip);
    return res.status(429).json({
      message: "Too many admin requests, please try again later.",
    });
  },
});

app.use(generalLimiter);

/* =================================================
   BODY PARSING
================================================= */
app.use(express.json({ limit: "20mb" }));
app.use(express.urlencoded({ extended: true, limit: "20mb" }));

/* =================================================
   REQUEST LOGGER (DEV ONLY)
================================================= */
if (IS_DEV) {
  app.use((req, res, next) => {
    console.log(
      "HIT:",
      req.method,
      req.originalUrl,
      "| origin:",
      req.headers.origin || "none",
      "| client:",
      req.headers["x-client"] || "none"
    );
    next();
  });

  app.use((req, res, next) => {
    const start = Date.now();
    res.on("finish", () => {
      const ms = Date.now() - start;
      console.log(`${req.method} ${req.originalUrl} - ${ms}ms`);
    });
    next();
  });
}

/* =================================================
   STATIC FILES
================================================= */
app.use(
  "/uploads",
  express.static(uploadsRoot, {
    fallthrough: true,
    index: false,
    maxAge: "1d",
  })
);

/* =================================================
   GLOBAL MAINTENANCE MODE
================================================= */
app.use(async (req, res, next) => {
  try {
    const requestPath = req.path || req.originalUrl || "";

    const alwaysAllow =
      requestPath === "/" ||
      requestPath === "/api/ping" ||
      requestPath.startsWith("/uploads");

    if (alwaysAllow) return next();

    if (!isDbReady()) {
      if (IS_DEV) console.warn("DB not ready, skipping maintenance check");
      return next();
    }

    const settings = await getCachedSettings();
    const maintenanceMode = !!settings?.maintenanceMode;
    const maintenanceMessage =
      settings?.maintenanceMessage ||
      "AutoAid is currently under maintenance. Only admin can access the system now.";

    if (!maintenanceMode) return next();

    if (requestPath.startsWith("/api/admin")) return next();

    const allowedAuthRoutesDuringMaintenance = [
      "/api/auth/login",
      "/api/auth/logout",
      "/api/auth/forgot-password",
      "/api/auth/reset-password",
    ];

    if (allowedAuthRoutesDuringMaintenance.includes(requestPath)) {
      return next();
    }

    const authHeader = req.headers.authorization || "";
    const bearer = authHeader.startsWith("Bearer ")
      ? authHeader.slice(7)
      : null;
    const token = bearer || req.cookies?.token || null;

    if (!token) {
      return res.status(503).json({
        ok: false,
        maintenanceMode: true,
        message: maintenanceMessage,
        systemName: settings?.systemName || "AutoAid",
        maintenanceTarget: settings?.maintenanceTarget || "both",
      });
    }

    try {
      if (!process.env.JWT_SECRET) {
        throw new Error("JWT_SECRET missing");
      }

      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      const user = await User.findById(decoded.id).select("role").lean();

      const isAdmin = String(user?.role || "").toLowerCase() === "admin";
      if (isAdmin) return next();
    } catch (tokenErr) {
      if (IS_DEV) {
        console.warn("Maintenance token check failed:", tokenErr.message);
      }
    }

    return res.status(503).json({
      ok: false,
      maintenanceMode: true,
      message: maintenanceMessage,
      systemName: settings?.systemName || "AutoAid",
      maintenanceTarget: settings?.maintenanceTarget || "both",
    });
  } catch (err) {
    console.error("Maintenance middleware error:", err.message);
    return next();
  }
});

/* =================================================
   HEALTH CHECK
================================================= */
app.get("/api/ping", (req, res) => {
  res.json({
    ok: true,
    message: "pong",
    dbReady: isDbReady(),
    time: new Date().toISOString(),
  });
});

app.get("/", (_req, res) => {
  res.send("AutoAID backend running successfully!");
});

/* =================================================
   SOCKET.IO
================================================= */
const io = new Server(server, {
  cors: {
    origin: (origin, callback) => {
      if (isAllowedOrigin(origin)) return callback(null, true);
      return callback(new Error(`Socket CORS blocked for origin: ${origin}`));
    },
    methods: ["GET", "POST"],
    credentials: true,
  },
});

app.set("io", io);

/* =================================================
   SOCKET AUTH
================================================= */
io.use(async (socket, next) => {
  try {
    if (!isDbReady()) {
      return next(new Error("Database temporarily unavailable"));
    }

    let token = null;

    if (socket.handshake.auth?.token) {
      token = socket.handshake.auth.token;
    }

    if (!token) {
      const authHeader =
        socket.handshake.headers.authorization ||
        socket.handshake.headers.Authorization;

      if (authHeader && authHeader.startsWith("Bearer ")) {
        token = authHeader.split(" ")[1];
      }
    }

    if (!token) {
      const cookiesHeader = socket.handshake.headers.cookie;
      if (cookiesHeader) {
        const parsed = cookie.parse(cookiesHeader);
        token = parsed.token;
      }
    }

    if (!token) return next(new Error("Not authenticated"));
    if (!process.env.JWT_SECRET) return next(new Error("JWT_SECRET missing"));

    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    const user = await User.findById(decoded.id).select("-password").lean();

    if (!user) return next(new Error("User not found"));

    const settings = await getCachedSettings();
    if (
      settings?.maintenanceMode &&
      String(user.role || "").toLowerCase() !== "admin"
    ) {
      return next(
        new Error(
          settings?.maintenanceMessage ||
            "AutoAid is currently under maintenance. Only admin can access the system now."
        )
      );
    }

    socket.user = user;
    next();
  } catch (e) {
    console.error("Socket auth error:", e.message);
    next(new Error("Authentication failed"));
  }
});

/* =================================================
   SOCKET HELPERS
================================================= */
function roomForChat(requestId) {
  return `chat_${requestId}`;
}

function normalizeBusinessType(value) {
  const v = String(value || "").trim().toLowerCase();
  if (["garage", "fuel", "towing", "ambulance"].includes(v)) return v;
  return "";
}

function getSenderRoleFromSocket(socket) {
  const role = String(socket.user?.role || "user").toLowerCase();
  if (role === "provider") return "provider";
  if (role === "admin") return "admin";
  return "user";
}

function getNotificationTitle(type) {
  if (type === "voice") return "New voice note";
  if (type === "call") return "Call update";
  if (type === "request") return "New service request";
  return "New message";
}

function getNotificationBody({ type, text, senderName, service, requestId }) {
  const prefix = senderName ? `${senderName}: ` : "";

  if (type === "voice") return `${prefix}🎤 Voice note`;
  if (type === "call") return `${prefix}${text || "📞 Call activity"}`;
  if (type === "request") {
    return `${service || "New service"} request${
      requestId ? ` #${String(requestId).slice(-6)}` : ""
    }`;
  }

  return `${prefix}${text || ""}`;
}

function getActiveTrackableStatuses() {
  return [
    "assigned",
    "driver_assigned",
    "mechanic_assigned",
    "vendor_assigned",
    "accepted",
    "confirmed",
    "on_the_way",
    "provider_on_the_way",
    "driver_on_the_way",
    "mechanic_on_the_way",
    "vendor_on_the_way",
    "arrived",
    "in_progress",
  ];
}

async function assertParticipant(socket, requestId) {
  if (!mongoose.Types.ObjectId.isValid(requestId)) {
    const err = new Error("Invalid request id");
    err.status = 400;
    throw err;
  }

  const reqDoc = await Request.findById(requestId);
  if (!reqDoc) {
    const err = new Error("Request not found");
    err.status = 404;
    throw err;
  }

  const me = String(socket.user?._id);
  const userId = String(reqDoc.userId || "");
  const providerId = String(reqDoc.assignedProviderId || "");

  const allowed = me === userId || (providerId && me === providerId);
  if (!allowed && String(socket.user?.role || "").toLowerCase() !== "admin") {
    const err = new Error("Not allowed");
    err.status = 403;
    throw err;
  }

  return reqDoc;
}

function emitPersonalNotification(ioInstance, reqDoc, payload) {
  const userId = String(reqDoc.userId || "");
  const providerId = String(reqDoc.assignedProviderId || "");

  if (userId) ioInstance.to(`user:${userId}`).emit("notify", payload);
  if (providerId) ioInstance.to(`provider:${providerId}`).emit("notify", payload);
}

function emitRequestLifecycle(ioInstance, requestDoc, eventName = "request_updated") {
  const providerType = normalizeBusinessType(requestDoc.providerType);
  const requestId = String(requestDoc._id || "");

  if (providerType) {
    ioInstance.to(`type:${providerType}`).emit(eventName, requestDoc);
  }

  if (requestDoc.targetProviderId) {
    ioInstance
      .to(`provider:${String(requestDoc.targetProviderId)}`)
      .emit(eventName, requestDoc);
  }

  if (requestDoc.assignedProviderId) {
    ioInstance
      .to(`provider:${String(requestDoc.assignedProviderId)}`)
      .emit(eventName, requestDoc);
  }

  if (requestDoc.userId) {
    ioInstance.to(`user:${String(requestDoc.userId)}`).emit(eventName, requestDoc);
  }

  if (requestId) {
    ioInstance.to(`request:${requestId}`).emit(eventName, requestDoc);
  }
}

function emitProviderLiveLocation(ioInstance, requestDoc, payload) {
  const requestId = String(requestDoc?._id || "");
  const userId = String(requestDoc?.userId || "");
  const assignedProviderId = String(requestDoc?.assignedProviderId || "");

  if (requestId) {
    ioInstance.to(`request:${requestId}`).emit("provider_live_location", payload);
  }

  if (userId) {
    ioInstance.to(`user:${userId}`).emit("provider_live_location", payload);
  }

  if (assignedProviderId) {
    ioInstance
      .to(`provider:${assignedProviderId}`)
      .emit("provider_live_location", payload);
  }
}

async function markProviderSocketOnline(userId, socketId) {
  const provider = await User.findById(userId);
  if (!provider) return null;

  if (String(provider.role || "").toLowerCase() === "provider") {
    provider.isOnline = true;
    provider.socketId = socketId;
    provider.lastSeenAt = new Date();
    await provider.save();
  }

  return provider;
}

async function markProviderSocketOffline(userId, socketId) {
  const provider = await User.findById(userId);
  if (!provider) return null;

  if (
    String(provider.role || "").toLowerCase() === "provider" &&
    String(provider.socketId || "") === String(socketId || "")
  ) {
    provider.isOnline = false;
    provider.socketId = "";
    provider.lastSeenAt = new Date();
    await provider.save();
  }

  return provider;
}

async function getEligibleProvidersForBroadcast(requestDoc) {
  const providerType = normalizeBusinessType(requestDoc.providerType);
  if (!providerType) return [];

  const filter = {
    role: "provider",
    status: { $in: ["approved", "active"] },
    isApprovedProvider: true,
    isAvailable: true,
    isOnline: true,
    businessType: providerType,
  };

  if (requestDoc.targetProviderId) {
    filter._id = requestDoc.targetProviderId;
  }

  return User.find(filter)
    .select("_id name businessType lat lng isAvailable isOnline socketId rating phone")
    .lean();
}

async function broadcastRequestToEligibleProviders(ioInstance, requestDoc) {
  const providers = await getEligibleProvidersForBroadcast(requestDoc);
  if (!providers.length) return { count: 0, providerIds: [] };

  const payload = {
    requestId: String(requestDoc._id),
    request: requestDoc,
    service: requestDoc.service,
    providerType: requestDoc.providerType,
    userLocation: requestDoc.userLocation || { lat: 0, lng: 0 },
    createdAt: requestDoc.createdAt || new Date(),
  };

  const targetedIds = [];

  for (const provider of providers) {
    const providerId = String(provider._id);
    targetedIds.push(providerId);

    ioInstance.to(`provider:${providerId}`).emit("new_request_broadcast", payload);
    ioInstance.to(`provider:${providerId}`).emit("notify", {
      id: `req_${requestDoc._id}_${providerId}`,
      type: "request",
      title: getNotificationTitle("request"),
      body: getNotificationBody({
        type: "request",
        service: requestDoc.service || requestDoc.providerType,
        requestId: requestDoc._id,
      }),
      requestId: String(requestDoc._id),
      createdAt: new Date().toISOString(),
    });
  }

  return { count: providers.length, providerIds: targetedIds };
}

/* =================================================
   SOCKET CHAT + NOTIFICATIONS + PROVIDER PRESENCE
================================================= */
io.on("connection", async (socket) => {
  if (IS_DEV) {
    console.log("Secure socket connected:", socket.id, "role:", socket.user?.role);
  }

  const myId = String(socket.user?._id || "");
  const myRole = String(socket.user?.role || "").toLowerCase();
  const myBusinessType = normalizeBusinessType(socket.user?.businessType);

  try {
    if (myId) {
      socket.join(`user:${myId}`);
      socket.join(`provider:${myId}`);

      if (IS_DEV) {
        console.log(
          "Joined personal rooms:",
          `user:${myId}`,
          `provider:${myId}`,
          "role:",
          myRole
        );
      }
    }

    if (myRole === "provider") {
      const provider = await markProviderSocketOnline(myId, socket.id);

      if (provider) {
        const providerType = normalizeBusinessType(provider.businessType);

        if (providerType) {
          socket.join(`type:${providerType}`);
          if (IS_DEV) {
            console.log(`Provider ${myId} joined service room type:${providerType}`);
          }
        }

        socket.emit("provider_presence", {
          isOnline: true,
          isAvailable: !!provider.isAvailable,
          businessType: providerType,
          socketId: socket.id,
        });
      }
    }
  } catch (presenceErr) {
    console.error("Provider presence init error:", presenceErr.message);
  }

  socket.on("join_request_room", async ({ requestId } = {}) => {
    try {
      if (!isDbReady()) {
        socket.emit("request_room_error", {
          requestId,
          message: "Database temporarily unavailable",
        });
        return;
      }

      if (!requestId || !mongoose.Types.ObjectId.isValid(requestId)) {
        socket.emit("request_room_error", {
          requestId,
          message: "Valid requestId is required",
        });
        return;
      }

      await assertParticipant(socket, requestId);
      socket.join(`request:${requestId}`);
      socket.emit("request_room_joined", { requestId });
    } catch (e) {
      socket.emit("request_room_error", {
        requestId,
        message: e.message || "Failed to join request room",
      });
    }
  });

  socket.on("provider_register_service_room", async ({ businessType }) => {
    try {
      if (!isDbReady()) {
        socket.emit("provider_error", { message: "Database temporarily unavailable" });
        return;
      }

      if (myRole !== "provider") {
        socket.emit("provider_error", { message: "Only providers can do this" });
        return;
      }

      const cleanType = normalizeBusinessType(businessType || myBusinessType);
      if (!cleanType) {
        socket.emit("provider_error", { message: "Invalid business type" });
        return;
      }

      socket.join(`type:${cleanType}`);

      await User.findByIdAndUpdate(myId, {
        $set: {
          businessType: cleanType,
          isOnline: true,
          socketId: socket.id,
          lastSeenAt: new Date(),
        },
      });

      socket.emit("provider_room_joined", {
        room: `type:${cleanType}`,
        businessType: cleanType,
      });
    } catch (e) {
      socket.emit("provider_error", {
        message: e.message || "Failed to join provider service room",
      });
    }
  });

  socket.on("provider_set_availability", async ({ isAvailable, lat, lng }) => {
    try {
      if (!isDbReady()) {
        socket.emit("provider_error", { message: "Database temporarily unavailable" });
        return;
      }

      if (myRole !== "provider") {
        socket.emit("provider_error", { message: "Only providers can update availability" });
        return;
      }

      const update = {
        isAvailable: !!isAvailable,
        isOnline: true,
        socketId: socket.id,
        lastSeenAt: new Date(),
      };

      if (typeof lat === "number" && Number.isFinite(lat)) update.lat = lat;
      if (typeof lng === "number" && Number.isFinite(lng)) update.lng = lng;

      const updated = await User.findByIdAndUpdate(
        myId,
        { $set: update },
        { new: true }
      )
        .select("-password")
        .lean();

      socket.emit("provider_presence", {
        isOnline: !!updated?.isOnline,
        isAvailable: !!updated?.isAvailable,
        lat: updated?.lat ?? 0,
        lng: updated?.lng ?? 0,
        businessType: normalizeBusinessType(updated?.businessType),
      });
    } catch (e) {
      socket.emit("provider_error", {
        message: e.message || "Failed to update availability",
      });
    }
  });

  socket.on("provider_update_location", async ({ lat, lng } = {}) => {
    try {
      if (!isDbReady()) {
        socket.emit("provider_error", { message: "Database temporarily unavailable" });
        return;
      }

      if (myRole !== "provider") {
        socket.emit("provider_error", { message: "Only providers can update location" });
        return;
      }

      const safeLat = Number(lat);
      const safeLng = Number(lng);

      if (!Number.isFinite(safeLat) || !Number.isFinite(safeLng)) {
        socket.emit("provider_error", { message: "Valid lat and lng are required" });
        return;
      }

      await User.findByIdAndUpdate(myId, {
        $set: {
          lat: safeLat,
          lng: safeLng,
          isOnline: true,
          socketId: socket.id,
          lastSeenAt: new Date(),
        },
      });

      const livePayload = {
        providerId: myId,
        lat: safeLat,
        lng: safeLng,
        updatedAt: new Date().toISOString(),
      };

      socket.emit("provider_location_updated", livePayload);

      const activeRequests = await Request.find({
        assignedProviderId: myId,
        status: { $in: getActiveTrackableStatuses() },
      })
        .select("_id userId assignedProviderId status service providerType userLocation")
        .limit(5)
        .lean();

      for (const requestDoc of activeRequests) {
        emitProviderLiveLocation(io, requestDoc, {
          ...livePayload,
          requestId: String(requestDoc._id),
          status: requestDoc.status || "",
          service: requestDoc.service || requestDoc.providerType || "",
          userLocation: requestDoc.userLocation || null,
        });
      }
    } catch (e) {
      socket.emit("provider_error", {
        message: e.message || "Failed to update location",
      });
    }
  });

  socket.on("provider_ping", async () => {
    try {
      if (!isDbReady()) return;
      if (myRole !== "provider") return;

      await User.findByIdAndUpdate(myId, {
        $set: {
          isOnline: true,
          socketId: socket.id,
          lastSeenAt: new Date(),
        },
      });
    } catch (e) {
      console.error("provider_ping error:", e.message);
    }
  });

  socket.on("broadcast_request_to_providers", async ({ requestId }) => {
    try {
      if (!isDbReady()) {
        socket.emit("provider_error", { message: "Database temporarily unavailable" });
        return;
      }

      if (!requestId || !mongoose.Types.ObjectId.isValid(requestId)) {
        socket.emit("provider_error", { message: "Valid requestId is required" });
        return;
      }

      const requestDoc = await Request.findById(requestId);
      if (!requestDoc) {
        socket.emit("provider_error", { message: "Request not found" });
        return;
      }

      const me = String(socket.user?._id || "");
      const isOwner = String(requestDoc.userId || "") === me;
      const admin = myRole === "admin";

      if (!isOwner && !admin) {
        socket.emit("provider_error", { message: "Not allowed to broadcast this request" });
        return;
      }

      const result = await broadcastRequestToEligibleProviders(io, requestDoc);

      socket.emit("broadcast_result", {
        requestId: String(requestDoc._id),
        matchedProviders: result.count,
        providerIds: result.providerIds,
      });
    } catch (e) {
      socket.emit("provider_error", {
        message: e.message || "Broadcast failed",
      });
    }
  });

  socket.on("joinChat", async ({ requestId }) => {
    try {
      if (!isDbReady()) {
        socket.emit("chat_error", {
          requestId,
          message: "Database temporarily unavailable",
        });
        return;
      }

      if (!requestId) return;

      const settings = await getCachedSettings();
      if (
        settings?.maintenanceMode &&
        String(socket.user?.role).toLowerCase() !== "admin"
      ) {
        socket.emit("chat_error", {
          requestId,
          message:
            settings?.maintenanceMessage ||
            "AutoAid is currently under maintenance. Only admin can access the system now.",
          maintenanceMode: true,
        });
        return;
      }

      await assertParticipant(socket, requestId);
      socket.join(roomForChat(requestId));
      socket.join(`request:${requestId}`);

      const rid = new mongoose.Types.ObjectId(requestId);

      const messages = await ChatMessage.find({ requestId: rid })
        .sort({ createdAt: 1 })
        .limit(200)
        .lean();

      socket.emit("chat_history", { requestId, messages });
      socket.emit("chat_joined", { requestId });
    } catch (e) {
      socket.emit("chat_error", {
        requestId,
        message: e.message || "Join failed",
      });
    }
  });

  socket.on("leaveChat", ({ requestId }) => {
    if (!requestId) return;
    socket.leave(roomForChat(requestId));
    socket.leave(`request:${requestId}`);
    socket.emit("chat_left", { requestId });
  });

  socket.on("sendMessage", async (payload = {}) => {
    const {
      requestId,
      type = "text",
      text = "",
      audioUrl = "",
      durationSec = 0,
      meta = {},
    } = payload;

    try {
      if (!isDbReady()) {
        socket.emit("chat_error", {
          requestId,
          message: "Database temporarily unavailable",
        });
        return;
      }

      const settings = await getCachedSettings();
      if (
        settings?.maintenanceMode &&
        String(socket.user?.role).toLowerCase() !== "admin"
      ) {
        socket.emit("chat_error", {
          requestId,
          message:
            settings?.maintenanceMessage ||
            "AutoAid is currently under maintenance. Only admin can access the system now.",
          maintenanceMode: true,
        });
        return;
      }

      if (!requestId) {
        socket.emit("chat_error", {
          requestId,
          message: "requestId is required",
        });
        return;
      }

      const reqDoc = await assertParticipant(socket, requestId);

      const status = String(reqDoc.status || "").toLowerCase();
      const canChat = [
        "assigned",
        "arrived",
        "quoted",
        "awaiting_payment",
        "in_progress",
        "completed",
      ].includes(status);

      const isAdminRole = String(socket.user?.role || "").toLowerCase() === "admin";

      if (!canChat && !isAdminRole) {
        socket.emit("chat_error", {
          requestId,
          message: "Chat is only available after a provider is assigned.",
        });
        return;
      }

      const cleanType = String(type || "text").trim().toLowerCase();
      const cleanText = String(text || "").trim();
      const cleanAudioUrl = String(audioUrl || "").trim();
      const safeDurationSec = Number(durationSec || 0);

      if (!["text", "voice", "call"].includes(cleanType)) {
        socket.emit("chat_error", {
          requestId,
          message: "Unsupported message type",
        });
        return;
      }

      if (cleanType === "text" && !cleanText) {
        socket.emit("chat_error", {
          requestId,
          message: "Text message cannot be empty",
        });
        return;
      }

      if (cleanType === "voice" && !cleanAudioUrl) {
        socket.emit("chat_error", {
          requestId,
          message: "Voice message audioUrl is required",
        });
        return;
      }

      const sender = getSenderRoleFromSocket(socket);
      const rid = new mongoose.Types.ObjectId(requestId);

      const msg = await ChatMessage.create({
        requestId: rid,
        sender,
        senderId: socket.user._id,
        type: cleanType,
        text: cleanText,
        audioUrl: cleanAudioUrl,
        durationSec: Number.isFinite(safeDurationSec) ? safeDurationSec : 0,
        meta: meta || {},
        readBy: [socket.user._id],
      });

      io.to(roomForChat(requestId)).emit("new_message", { requestId, message: msg });

      const notifyPayload = {
        id: `msg_${msg._id}`,
        type: cleanType,
        title: getNotificationTitle(cleanType),
        body: getNotificationBody({
          type: cleanType,
          text: cleanText,
          senderName: socket.user?.name || "",
        }),
        requestId,
        createdAt: new Date().toISOString(),
      };

      socket.to(roomForChat(requestId)).emit("notify", notifyPayload);
      emitPersonalNotification(io, reqDoc, notifyPayload);
    } catch (e) {
      socket.emit("chat_error", {
        requestId,
        message: e.message || "Send failed",
      });
    }
  });

  socket.on("call_request", async ({ requestId, from }) => {
    try {
      if (!isDbReady()) {
        socket.emit("chat_error", {
          requestId,
          message: "Database temporarily unavailable",
        });
        return;
      }

      if (!requestId) {
        socket.emit("chat_error", { requestId, message: "requestId is required" });
        return;
      }

      const settings = await getCachedSettings();
      if (
        settings?.maintenanceMode &&
        String(socket.user?.role).toLowerCase() !== "admin"
      ) {
        socket.emit("chat_error", {
          requestId,
          message:
            settings?.maintenanceMessage ||
            "AutoAid is currently under maintenance. Only admin can access the system now.",
          maintenanceMode: true,
        });
        return;
      }

      const reqDoc = await assertParticipant(socket, requestId);

      const payload = {
        id: `call_req_${Date.now()}`,
        type: "call",
        title: "Incoming call",
        body: `${socket.user?.name || "Someone"} is calling`,
        requestId,
        createdAt: new Date().toISOString(),
      };

      io.to(roomForChat(requestId)).emit("incoming_call", {
        requestId,
        from: from || getSenderRoleFromSocket(socket),
        createdAt: new Date().toISOString(),
      });

      emitPersonalNotification(io, reqDoc, payload);
    } catch (e) {
      socket.emit("chat_error", {
        requestId,
        message: e.message || "Call request failed",
      });
    }
  });

  socket.on("call_user", async ({ requestId, offer }) => {
    try {
      if (!isDbReady()) {
        socket.emit("chat_error", {
          requestId,
          message: "Database temporarily unavailable",
        });
        return;
      }

      if (!requestId) {
        socket.emit("chat_error", { requestId, message: "requestId is required" });
        return;
      }

      const settings = await getCachedSettings();
      if (
        settings?.maintenanceMode &&
        String(socket.user?.role).toLowerCase() !== "admin"
      ) {
        socket.emit("chat_error", {
          requestId,
          message:
            settings?.maintenanceMessage ||
            "AutoAid is currently under maintenance. Only admin can access the system now.",
          maintenanceMode: true,
        });
        return;
      }

      const reqDoc = await assertParticipant(socket, requestId);

      socket.to(roomForChat(requestId)).emit("incoming_call", {
        requestId,
        from: getSenderRoleFromSocket(socket),
        offer,
        createdAt: new Date().toISOString(),
      });

      emitPersonalNotification(io, reqDoc, {
        id: `call_user_${Date.now()}`,
        type: "call",
        title: "Incoming call",
        body: `${socket.user?.name || "Someone"} is calling`,
        requestId,
        createdAt: new Date().toISOString(),
      });
    } catch (e) {
      socket.emit("chat_error", {
        requestId,
        message: e.message || "Call start failed",
      });
    }
  });

  socket.on("answer_call", async ({ requestId, answer }) => {
    try {
      if (!isDbReady()) {
        socket.emit("chat_error", {
          requestId,
          message: "Database temporarily unavailable",
        });
        return;
      }

      if (!requestId) {
        socket.emit("chat_error", { requestId, message: "requestId is required" });
        return;
      }

      const settings = await getCachedSettings();
      if (
        settings?.maintenanceMode &&
        String(socket.user?.role).toLowerCase() !== "admin"
      ) {
        socket.emit("chat_error", {
          requestId,
          message:
            settings?.maintenanceMessage ||
            "AutoAid is currently under maintenance. Only admin can access the system now.",
          maintenanceMode: true,
        });
        return;
      }

      await assertParticipant(socket, requestId);

      socket.to(roomForChat(requestId)).emit("call_answered", {
        requestId,
        by: getSenderRoleFromSocket(socket),
        answer,
        createdAt: new Date().toISOString(),
      });
    } catch (e) {
      socket.emit("chat_error", {
        requestId,
        message: e.message || "Answer failed",
      });
    }
  });

  socket.on("ice_candidate", async ({ requestId, candidate }) => {
    try {
      if (!isDbReady()) {
        socket.emit("chat_error", {
          requestId,
          message: "Database temporarily unavailable",
        });
        return;
      }

      if (!requestId) {
        socket.emit("chat_error", { requestId, message: "requestId is required" });
        return;
      }

      const settings = await getCachedSettings();
      if (
        settings?.maintenanceMode &&
        String(socket.user?.role).toLowerCase() !== "admin"
      ) {
        socket.emit("chat_error", {
          requestId,
          message:
            settings?.maintenanceMessage ||
            "AutoAid is currently under maintenance. Only admin can access the system now.",
          maintenanceMode: true,
        });
        return;
      }

      await assertParticipant(socket, requestId);

      socket.to(roomForChat(requestId)).emit("ice_candidate", {
        requestId,
        candidate,
      });
    } catch (e) {
      socket.emit("chat_error", {
        requestId,
        message: e.message || "ICE candidate failed",
      });
    }
  });

  socket.on("end_call", async ({ requestId }) => {
    try {
      if (!isDbReady()) {
        socket.emit("chat_error", {
          requestId,
          message: "Database temporarily unavailable",
        });
        return;
      }

      if (!requestId) {
        socket.emit("chat_error", { requestId, message: "requestId is required" });
        return;
      }

      const settings = await getCachedSettings();
      if (
        settings?.maintenanceMode &&
        String(socket.user?.role).toLowerCase() !== "admin"
      ) {
        socket.emit("chat_error", {
          requestId,
          message:
            settings?.maintenanceMessage ||
            "AutoAid is currently under maintenance. Only admin can access the system now.",
          maintenanceMode: true,
        });
        return;
      }

      const reqDoc = await assertParticipant(socket, requestId);

      io.to(roomForChat(requestId)).emit("call_ended", {
        requestId,
        by: getSenderRoleFromSocket(socket),
        createdAt: new Date().toISOString(),
      });

      emitPersonalNotification(io, reqDoc, {
        id: `call_end_${Date.now()}`,
        type: "call",
        title: "Call ended",
        body: `${socket.user?.name || "Someone"} ended the call`,
        requestId,
        createdAt: new Date().toISOString(),
      });
    } catch (e) {
      socket.emit("chat_error", {
        requestId,
        message: e.message || "End call failed",
      });
    }
  });

  socket.on("markRead", async ({ requestId }) => {
    try {
      if (!isDbReady()) {
        socket.emit("chat_error", {
          requestId,
          message: "Database temporarily unavailable",
        });
        return;
      }

      const settings = await getCachedSettings();
      if (
        settings?.maintenanceMode &&
        String(socket.user?.role).toLowerCase() !== "admin"
      ) {
        socket.emit("chat_error", {
          requestId,
          message:
            settings?.maintenanceMessage ||
            "AutoAid is currently under maintenance. Only admin can access the system now.",
          maintenanceMode: true,
        });
        return;
      }

      if (!requestId) return;

      await assertParticipant(socket, requestId);

      const rid = new mongoose.Types.ObjectId(requestId);

      await ChatMessage.updateMany(
        { requestId: rid, readBy: { $ne: socket.user._id } },
        { $addToSet: { readBy: socket.user._id } }
      );

      socket.emit("read_ok", { requestId });
    } catch (e) {
      socket.emit("chat_error", {
        requestId,
        message: e.message || "Read update failed",
      });
    }
  });

  socket.on("get_notifications", async () => {
    try {
      if (!isDbReady()) {
        socket.emit("notifications_error", {
          message: "Database temporarily unavailable",
        });
        return;
      }

      const settings = await getCachedSettings();
      if (
        settings?.maintenanceMode &&
        String(socket.user?.role).toLowerCase() !== "admin"
      ) {
        socket.emit("notifications_error", {
          maintenanceMode: true,
          message:
            settings?.maintenanceMessage ||
            "AutoAid is currently under maintenance. Only admin can access the system now.",
        });
        return;
      }

      const myObjectId = socket.user._id;

      const requestFilter =
        myRole === "provider"
          ? { assignedProviderId: myObjectId }
          : myRole === "admin"
          ? {}
          : { userId: myObjectId };

      const myRequests = await Request.find(requestFilter)
        .select("_id createdAt status")
        .sort({ createdAt: -1 })
        .limit(50)
        .lean();

      const requestIds = myRequests.map((r) => r._id);

      const unread = await ChatMessage.aggregate([
        {
          $match: {
            requestId: { $in: requestIds },
            readBy: { $ne: myObjectId },
          },
        },
        { $sort: { createdAt: -1 } },
        { $limit: 100 },
        {
          $lookup: {
            from: "users",
            localField: "senderId",
            foreignField: "_id",
            as: "senderUser",
          },
        },
        { $unwind: { path: "$senderUser", preserveNullAndEmptyArrays: true } },
        {
          $project: {
            _id: 1,
            requestId: 1,
            type: 1,
            text: 1,
            audioUrl: 1,
            durationSec: 1,
            createdAt: 1,
            senderName: "$senderUser.name",
          },
        },
      ]);

      const notifications = unread.map((m) => ({
        id: String(m._id),
        type: String(m.type || "text"),
        title: getNotificationTitle(String(m.type || "text")),
        body: getNotificationBody({
          type: String(m.type || "text"),
          text: String(m.text || ""),
          senderName: m.senderName,
        }),
        requestId: String(m.requestId),
        createdAt: m.createdAt,
      }));

      socket.emit("notifications", { notifications });
    } catch (e) {
      console.error("notifications_error:", e);
      socket.emit("notifications_error", {
        message: e.message || "Failed to load notifications",
      });
    }
  });

  socket.on("request_status_refresh", async ({ requestId }) => {
    try {
      if (!isDbReady()) return;
      if (!requestId || !mongoose.Types.ObjectId.isValid(requestId)) return;

      const requestDoc = await Request.findById(requestId);
      if (!requestDoc) return;

      emitRequestLifecycle(io, requestDoc, "request_updated");
    } catch (e) {
      console.error("request_status_refresh error:", e.message);
    }
  });

  socket.on("disconnect", async () => {
    if (IS_DEV) {
      console.log("Socket disconnected:", socket.id);
    }

    try {
      if (!isDbReady()) return;

      if (myRole === "provider" && myId) {
        await markProviderSocketOffline(myId, socket.id);
      }
    } catch (e) {
      console.error("disconnect cleanup error:", e.message);
    }
  });
});

/* =================================================
   DATABASE
================================================= */
const PORT = process.env.PORT || 5001;
const MONGO_URI = process.env.MONGO_URI;

async function ensureDefaultSettings() {
  try {
    const settings = await Settings.findOne();

    if (!settings) {
      await Settings.create({
        systemName: "AutoAid",
        supportEmail: "",
        supportPhone: "",
        whatsappNumber: "",
        emergencyHotline: "",
        notificationsEnabled: true,
        maintenanceMode: false,
        maintenanceMessage:
          "AutoAid is currently under maintenance. Only admin can access the system now.",
        maintenanceTarget: "both",
        allowUserRegistration: true,
        allowProviderRegistration: true,
        autoApproveProviders: false,
      });
      invalidateSettingsCache();
      console.log("Default settings created");
    } else {
      let changed = false;

      if (typeof settings.supportPhone === "undefined") {
        settings.supportPhone = "";
        changed = true;
      }
      if (typeof settings.whatsappNumber === "undefined") {
        settings.whatsappNumber = "";
        changed = true;
      }
      if (typeof settings.emergencyHotline === "undefined") {
        settings.emergencyHotline = "";
        changed = true;
      }
      if (typeof settings.maintenanceTarget === "undefined") {
        settings.maintenanceTarget = "both";
        changed = true;
      }
      if (typeof settings.allowUserRegistration === "undefined") {
        settings.allowUserRegistration = true;
        changed = true;
      }
      if (typeof settings.allowProviderRegistration === "undefined") {
        settings.allowProviderRegistration = true;
        changed = true;
      }
      if (typeof settings.autoApproveProviders === "undefined") {
        settings.autoApproveProviders = false;
        changed = true;
      }

      if (changed) {
        await settings.save();
        invalidateSettingsCache();
        console.log("Settings schema backfilled with new defaults");
      }
    }
  } catch (err) {
    console.error("Default settings creation failed:", err.message);
  }
}

async function connectDb() {
  try {
    if (!MONGO_URI) {
      console.error("MONGO_URI missing in .env");
      process.exit(1);
    }

    mongoose.set("strictQuery", true);

    console.log("Connecting to MongoDB Atlas...");
    console.log(
      "Mongo host:",
      MONGO_URI.includes("@") ? MONGO_URI.split("@")[1]?.split("/")[0] : "unknown"
    );

    await mongoose.connect(MONGO_URI, {
      serverSelectionTimeoutMS: 15000,
      socketTimeoutMS: 45000,
    });

    console.log("MongoDB connected");
    console.log("Ready state:", mongoose.connection.readyState);

    mongoose.connection.on("error", (err) => {
      console.error("MongoDB runtime error:", err.message);
    });

    mongoose.connection.on("disconnected", () => {
      console.warn("MongoDB disconnected");
    });

    await ensureDefaultSettings();
    await getCachedSettings(true);
  } catch (err) {
    console.error("DB connection failed:", err.message || err);

    const msg = String(err.message || "").toLowerCase();

    if (msg.includes("whitelist") || msg.includes("could not connect to any servers")) {
      console.error(
        "Fix: Add your current IP in MongoDB Atlas Network Access, or temporarily allow 0.0.0.0/0."
      );
    }

    if (msg.includes("authentication failed") || msg.includes("bad auth")) {
      console.error("Fix: Check Atlas database username/password in MONGO_URI.");
    }

    if (msg.includes("querysrv econnrefused") || msg.includes("enotfound")) {
      console.error(
        "Fix: DNS/network issue. Try Google DNS, Cloudflare DNS, or non-SRV URI."
      );
    }

    process.exit(1);
  }
}

/* =================================================
   SUBSCRIPTION CHECK
================================================= */
async function checkAndExpireSubscriptions() {
  try {
    if (!isDbReady()) return;

    const now = new Date();

    const providers = await User.find({
      role: "provider",
      "subscription.active": true,
      "subscription.expiryDate": { $ne: null },
    }).select("_id subscription");

    for (const p of providers) {
      if (p.subscription?.expiryDate && p.subscription.expiryDate < now) {
        p.subscription.active = false;
        await p.save();
      }
    }
  } catch (err) {
    console.error("Subscription expiry error:", err.message || err);
  }
}

setInterval(checkAndExpireSubscriptions, 60 * 1000);

/* =================================================
   BOOT
================================================= */
(async function boot() {
  await connectDb();

  const uploadsRoutes = await loadOptionalRoute("./routes/uploads.js", "uploads");
  const voiceUploadRoutes = await loadOptionalRoute("./routes/voiceUpload.js", "voice upload");

  /* =================================================
     CORE ROUTES
  ================================================= */
  app.use("/api/navigation", navigationRoutes);
  app.use("/api/auth", authLimiter, authRoutes);
  app.use("/api/admin", adminLimiter, adminRoutes);
  app.use("/api/admin", adminLimiter, settingsRoutes);
  app.use("/api/payments", paymentsRoutes);
  app.use("/api/user-wallet", userWalletRoutes);
  app.use("/api/providers", providersRoutes);
  app.use("/api/fuel", fuelRoutes);
  app.use("/api/towing", towingRoutes);
  app.use("/api/ambulance", ambulanceRoutes);
  app.use("/api/garage", garageRoutes);
  app.use("/api/requests", requestsRoutes);
  app.use("/api/chat", chatRoutes);
  app.use("/api/ai", aiRoutes);
  app.use("/api/extra-charges", extraChargesRoutes);
  app.use("/api/referrals", referralRoutes);
  app.use("/api/subscriptions", subscriptionRoutes);
  app.use("/api/user-verification", userVerificationRoutes);
app.use("/api/provider-verification", providerVerificationRoutes);

  // verification routes
  app.use("/api/verification", verificationRoutes);
  app.use("/api/provider-verification", verificationRoutes);

  if (uploadsRoutes) {
    app.use("/api/uploads", uploadsRoutes);
  } else {
    app.use("/api/uploads", (_req, res) => {
      res.status(501).json({
        ok: false,
        message: "Uploads route not configured yet. Create routes/uploads.js",
      });
    });
  }

  if (voiceUploadRoutes) {
    app.use("/api/upload", voiceUploadRoutes);
  } else {
    app.use("/api/upload", (_req, res) => {
      res.status(501).json({
        ok: false,
        message: "Voice upload route not configured yet. Create routes/voiceUpload.js",
      });
    });
  }

  /* =================================================
     404 HANDLER
  ================================================= */
  app.use((req, res) => {
    res.status(404).json({
      message: `Route not found: ${req.method} ${req.originalUrl}`,
    });
  });

  /* =================================================
     ERROR HANDLER
  ================================================= */
  app.use((err, req, res, _next) => {
    console.error("Express error:", err);

    const origin = req.headers.origin;
    if (isAllowedOrigin(origin)) {
      res.header("Access-Control-Allow-Origin", origin || "*");
      res.header("Vary", "Origin");
      res.header("Access-Control-Allow-Credentials", "true");
    }

    if (String(err.message || "").includes("CORS")) {
      return res.status(403).json({ message: err.message });
    }

    return res.status(err.status || 500).json({
      message: err.message || "Server error",
    });
  });

  try {
    const adminEmail = "admin@autoaid.com";
    const exists = await User.findOne({ email: adminEmail }).lean();

    if (!exists) {
      if (!process.env.ADMIN_PASSWORD) {
        console.warn("ADMIN_PASSWORD missing in .env (admin not created)");
      } else {
        await new User({
          name: "System Admin",
          email: adminEmail,
          password: process.env.ADMIN_PASSWORD,
          role: "admin",
          status: "approved",
          verificationStatus: "verified",
          subscription: {
            plan: "monthly",
            active: false,
            startDate: null,
            expiryDate: null,
            paymentMethod: null,
            price: 0,
          },
        }).save();

        console.log("Admin created:", adminEmail);
      }
    }
  } catch (err) {
    console.error("Admin creation error:", err.message || err);
  }

  server.listen(PORT, "0.0.0.0", () => {
    console.log(`Server listening on http://0.0.0.0:${PORT}`);
    console.log(`Test ping: http://127.0.0.1:${PORT}/api/ping`);
    console.log(`Uploads served from: http://127.0.0.1:${PORT}/uploads`);
  });
})();