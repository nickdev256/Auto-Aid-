// server.js
// FULL UPDATED VERSION
// ANDROID + WEB FRIENDLY + REALTIME CHAT + NOTIFICATIONS + CORS FIX
// MAINTENANCE MODE (ADMIN-ONLY BYPASS) + VOICE NOTES + CALL SIGNALING
// PROVIDER ONLINE/OFFLINE + AVAILABILITY + REQUEST BROADCAST

import dotenv from "dotenv";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// ✅ Force load .env from same folder as server.js
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
import uploadsRoutes from "./routes/uploads.js";
import voiceUploadRoutes from "./routes/voiceUpload.js";
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

// MODELS
import User from "./models/User.js";
import ChatMessage from "./models/chat.js";
import Request from "./models/Request.js";
import Settings from "./models/Settings.js";

const app = express();
const server = http.createServer(app);

console.log("✅ ENV FILE LOADED. MONGO_URI exists =", !!process.env.MONGO_URI);

/* =================================================
   ✅ HELPERS
================================================= */
function isDbReady() {
  return mongoose.connection.readyState === 1;
}

/* =================================================
   ✅ CORS HELPERS
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
  console.warn("❌ CORS blocked origin:", origin);
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
   ✅ VERY IMPORTANT: CORS FIRST
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
    console.log(
      "✅ PREFLIGHT:",
      req.method,
      req.originalUrl,
      "Origin:",
      origin || "none"
    );
    return res.sendStatus(204);
  }

  next();
});

app.use(cors(corsOptions));

/* =================================================
   ✅ SECURITY
================================================= */
app.use(
  helmet({
    crossOriginResourcePolicy: false,
  })
);

app.use(cookieParser());

/* =================================================
   ✅ RATE LIMITS
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
    console.warn("⚠️ Rate limit hit:", req.method, req.originalUrl, "IP:", req.ip);
    return res.status(429).json({
      message: "Too many admin requests, please try again later.",
    });
  },
});

app.use(generalLimiter);

/* =================================================
   ✅ BODY PARSING
================================================= */
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

/* =================================================
   ✅ REQUEST LOGGER
================================================= */
app.use((req, res, next) => {
  console.log(
    "📱 HIT:",
    req.method,
    req.originalUrl,
    "| origin:",
    req.headers.origin || "none",
    "| client:",
    req.headers["x-client"] || "none"
  );
  next();
});

/* =================================================
   ✅ GLOBAL MAINTENANCE MODE
   ONLY ADMIN CAN ACCESS DURING MAINTENANCE
================================================= */
app.use(async (req, res, next) => {
  try {
    const requestPath = req.path || req.originalUrl || "";

    const alwaysAllow =
      requestPath === "/" ||
      requestPath === "/api/ping" ||
      requestPath.startsWith("/uploads");

    if (alwaysAllow) {
      return next();
    }

    // ✅ If DB is not ready, skip maintenance DB lookup
    // so the whole server doesn't keep throwing noisy errors
    if (!isDbReady()) {
      console.warn("⚠️ DB not ready, skipping maintenance check");
      return next();
    }

    const settings = await Settings.findOne().lean();

    const maintenanceMode = !!settings?.maintenanceMode;
    const maintenanceMessage =
      settings?.maintenanceMessage ||
      "AutoAid is currently under maintenance. Only admin can access the system now.";

    if (!maintenanceMode) {
      return next();
    }

    if (requestPath.startsWith("/api/admin")) {
      return next();
    }

    if (requestPath === "/api/auth/login") {
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
      });
    }

    try {
      if (!process.env.JWT_SECRET) {
        throw new Error("JWT_SECRET missing");
      }

      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      const user = await User.findById(decoded.id).select("role");

      const isAdmin = String(user?.role || "").toLowerCase() === "admin";

      if (isAdmin) {
        return next();
      }
    } catch (tokenErr) {
      console.warn("⚠️ Maintenance token check failed:", tokenErr.message);
    }

    return res.status(503).json({
      ok: false,
      maintenanceMode: true,
      message: maintenanceMessage,
      systemName: settings?.systemName || "AutoAid",
    });
  } catch (err) {
    console.error("❌ Maintenance middleware error:", err.message);
    return next();
  }
});

/* =================================================
   ✅ HEALTH CHECK
================================================= */
app.get("/api/ping", (req, res) => {
  res.json({
    ok: true,
    message: "pong",
    dbReady: isDbReady(),
    time: new Date().toISOString(),
  });
});

app.get("/", (req, res) => {
  res.send("🚀 AutoAID backend running successfully!");
});

/* =================================================
   ✅ SOCKET.IO
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
   ✅ SOCKET AUTH
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
    const user = await User.findById(decoded.id).select("-password");

    if (!user) return next(new Error("User not found"));

    const settings = await Settings.findOne().lean();
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
    console.error("❌ Socket auth error:", e.message);
    next(new Error("Authentication failed"));
  }
});

/* =================================================
   ✅ SOCKET HELPERS
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

  if (userId) {
    ioInstance.to(`user:${userId}`).emit("notify", payload);
  }

  if (providerId) {
    ioInstance.to(`provider:${providerId}`).emit("notify", payload);
  }
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

  return User.find(filter).select(
    "_id name businessType lat lng isAvailable isOnline socketId rating phone"
  );
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
   ✅ SOCKET CHAT + NOTIFICATIONS + PROVIDER PRESENCE
================================================= */
io.on("connection", async (socket) => {
  console.log("⚡ Secure socket connected:", socket.id, "role:", socket.user?.role);

  const myId = String(socket.user?._id || "");
  const myRole = String(socket.user?.role || "").toLowerCase();
  const myBusinessType = normalizeBusinessType(socket.user?.businessType);

  try {
    if (myId) {
      socket.join(`user:${myId}`);
      socket.join(`provider:${myId}`);
      console.log(
        "✅ Joined personal rooms:",
        `user:${myId}`,
        `provider:${myId}`,
        "role:",
        myRole
      );
    }

    if (myRole === "provider") {
      const provider = await markProviderSocketOnline(myId, socket.id);

      if (provider) {
        const providerType = normalizeBusinessType(provider.businessType);

        if (providerType) {
          socket.join(`type:${providerType}`);
          console.log(`✅ Provider ${myId} joined service room type:${providerType}`);
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
    console.error("❌ Provider presence init error:", presenceErr.message);
  }

  /* -----------------------------
     PROVIDER PRESENCE / AVAILABILITY
  ----------------------------- */
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

      const updated = await User.findByIdAndUpdate(myId, { $set: update }, { new: true }).select(
        "-password"
      );

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

  socket.on("provider_update_location", async ({ lat, lng }) => {
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

      socket.emit("provider_location_updated", {
        lat: safeLat,
        lng: safeLng,
      });
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

  /* -----------------------------
     REQUEST BROADCAST
  ----------------------------- */
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

  /* -----------------------------
     CHAT
  ----------------------------- */
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

      const settings = await Settings.findOne().lean();
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
        .limit(200);

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

      const settings = await Settings.findOne().lean();
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

      const isAdmin = String(socket.user?.role || "").toLowerCase() === "admin";

      if (!canChat && !isAdmin) {
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

  /* -----------------------------
     CALLS
  ----------------------------- */
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

      const settings = await Settings.findOne().lean();
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

      const settings = await Settings.findOne().lean();
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

      const settings = await Settings.findOne().lean();
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

      const settings = await Settings.findOne().lean();
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

      const settings = await Settings.findOne().lean();
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

  /* -----------------------------
     READS
  ----------------------------- */
  socket.on("markRead", async ({ requestId }) => {
    try {
      if (!isDbReady()) {
        socket.emit("chat_error", {
          requestId,
          message: "Database temporarily unavailable",
        });
        return;
      }

      const settings = await Settings.findOne().lean();
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

  /* -----------------------------
     NOTIFICATIONS
  ----------------------------- */
  socket.on("get_notifications", async () => {
    try {
      if (!isDbReady()) {
        socket.emit("notifications_error", {
          message: "Database temporarily unavailable",
        });
        return;
      }

      const settings = await Settings.findOne().lean();
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
        .limit(50);

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
      console.error("❌ notifications_error:", e);
      socket.emit("notifications_error", {
        message: e.message || "Failed to load notifications",
      });
    }
  });

  /* -----------------------------
     REQUEST EVENT BRIDGE
  ----------------------------- */
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

  /* -----------------------------
     DISCONNECT
  ----------------------------- */
  socket.on("disconnect", async () => {
    console.log("❌ Socket disconnected:", socket.id);

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
   ✅ DATABASE
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
        notificationsEnabled: true,
        maintenanceMode: false,
        maintenanceMessage:
          "AutoAid is currently under maintenance. Only admin can access the system now.",
      });
      console.log("✅ Default settings created");
    }
  } catch (err) {
    console.error("❌ Default settings creation failed:", err.message);
  }
}

async function connectDb() {
  try {
    if (!MONGO_URI) {
      console.error("❌ MONGO_URI missing in .env");
      process.exit(1);
    }

    console.log("⏳ Connecting to MongoDB Atlas...");
    console.log(
      "🔎 Mongo host:",
      MONGO_URI.includes("@") ? MONGO_URI.split("@")[1]?.split("/")[0] : "unknown"
    );

    await mongoose.connect(MONGO_URI, {
      serverSelectionTimeoutMS: 15000,
      socketTimeoutMS: 45000,
    });

    console.log("✅ MongoDB connected");
    console.log("✅ Ready state:", mongoose.connection.readyState);

    mongoose.connection.on("error", (err) => {
      console.error("❌ MongoDB runtime error:", err.message);
    });

    mongoose.connection.on("disconnected", () => {
      console.warn("⚠️ MongoDB disconnected");
    });

    await ensureDefaultSettings();
  } catch (err) {
    console.error("❌ DB connection failed:", err.message || err);

    const msg = String(err.message || "").toLowerCase();

    if (msg.includes("whitelist") || msg.includes("could not connect to any servers")) {
      console.error(
        "👉 Fix: Add your current IP in MongoDB Atlas Network Access, or temporarily allow 0.0.0.0/0."
      );
    }

    if (msg.includes("authentication failed") || msg.includes("bad auth")) {
      console.error("👉 Fix: Check Atlas database username/password in MONGO_URI.");
    }

    if (msg.includes("querysrv econnrefused") || msg.includes("enotfound")) {
      console.error(
        "👉 Fix: DNS/network issue. Try Google DNS, Cloudflare DNS, or non-SRV URI."
      );
    }

    process.exit(1);
  }
}

/* =================================================
   ✅ SUBSCRIPTION CHECK
================================================= */
async function checkAndExpireSubscriptions() {
  try {
    if (!isDbReady()) return;

    const now = new Date();

    const providers = await User.find({
      role: "provider",
      "subscription.active": true,
      "subscription.expiryDate": { $ne: null },
    });

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
   ✅ ROUTES
================================================= */
app.use("/api/auth", authLimiter, authRoutes);
app.use("/api/admin", adminLimiter, adminRoutes);
app.use("/api/payments", paymentsRoutes);
app.use("/api/providers", providersRoutes);
app.use("/api/fuel", fuelRoutes);
app.use("/api/towing", towingRoutes);
app.use("/api/ambulance", ambulanceRoutes);
app.use("/api/garage", garageRoutes);
app.use("/api/uploads", uploadsRoutes);
app.use("/api/upload", voiceUploadRoutes);
app.use("/api/requests", requestsRoutes);
app.use("/api/chat", chatRoutes);

app.use("/uploads", express.static(path.join(__dirname, "uploads")));

/* =================================================
   ✅ 404 HANDLER
================================================= */
app.use((req, res) => {
  res.status(404).json({
    message: `Route not found: ${req.method} ${req.originalUrl}`,
  });
});

/* =================================================
   ✅ ERROR HANDLER
================================================= */
app.use((err, req, res, next) => {
  console.error("❌ Express error:", err);

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

/* =================================================
   ✅ BOOT
================================================= */
(async function boot() {
  await connectDb();

  try {
    const adminEmail = "admin@autoaid.com";
    const exists = await User.findOne({ email: adminEmail });

    if (!exists) {
      if (!process.env.ADMIN_PASSWORD) {
        console.warn("⚠️ ADMIN_PASSWORD missing in .env (admin not created)");
      } else {
        await new User({
          name: "System Admin",
          email: adminEmail,
          password: process.env.ADMIN_PASSWORD,
          role: "admin",
          status: "approved",
          subscription: {
            plan: "monthly",
            active: false,
            startDate: null,
            expiryDate: null,
            paymentMethod: null,
            price: 0,
          },
        }).save();

        console.log("✅ Admin created:", adminEmail);
      }
    }
  } catch (err) {
    console.error("Admin creation error:", err.message || err);
  }

  server.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 Server listening on http://0.0.0.0:${PORT}`);
    console.log(`✅ Test ping: http://127.0.0.1:${PORT}/api/ping`);
  });
})();