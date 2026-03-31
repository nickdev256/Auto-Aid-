// socket.js
import { Server } from "socket.io";
import jwt from "jsonwebtoken";
import cookie from "cookie";
import mongoose from "mongoose";

import User from "./models/User.js";
import Request from "./models/Request.js";
import ChatMessage from "./models/chat.js";
import Settings from "./models/Settings.js";

export function initSocket(server, allowOrigin) {
  const io = new Server(server, {
    cors: {
      origin: allowOrigin,
      methods: ["GET", "POST"],
      credentials: true,
    },
  });

  /* =================================================
     HELPERS
  ================================================= */
  function isDbReady() {
    return mongoose.connection.readyState === 1;
  }

  function roomForChat(requestId) {
    return `chat_${requestId}`;
  }

  function normalizeBusinessType(value = "") {
    const v = String(value || "").trim().toLowerCase();

    if (["garage", "mechanic", "repair"].includes(v)) return "garage";
    if (["fuel", "fuel delivery", "petrol", "diesel"].includes(v)) return "fuel";
    if (["towing", "tow", "towing track", "towing truck"].includes(v)) return "towing";
    if (["ambulance", "medical", "emergency"].includes(v)) return "ambulance";

    return "";
  }

  function isSupportedBusinessType(value = "") {
    return ["garage", "fuel", "towing", "ambulance"].includes(
      normalizeBusinessType(value)
    );
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

    const me = String(socket.user?._id || "");
    const userId = String(reqDoc.userId || "");
    const providerId = String(reqDoc.assignedProviderId || reqDoc.assignedTo || "");

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
    const providerId = String(reqDoc.assignedProviderId || reqDoc.assignedTo || "");

    if (userId) {
      ioInstance.to(`user:${userId}`).emit("notify", payload);
    }

    if (providerId) {
      ioInstance.to(`provider:${providerId}`).emit("notify", payload);
    }
  }

  function emitRequestLifecycle(ioInstance, requestDoc, eventName = "request_updated") {
    const providerType = normalizeBusinessType(requestDoc.providerType || requestDoc.service);
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

    if (requestDoc.assignedTo) {
      ioInstance
        .to(`provider:${String(requestDoc.assignedTo)}`)
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
      const normalizedType = normalizeBusinessType(
        provider.businessType || provider.providerType || provider.serviceType || ""
      );

      provider.businessType = normalizedType || provider.businessType || "";
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
    const providerType = normalizeBusinessType(requestDoc.providerType || requestDoc.service);
    if (!providerType) return [];

    const baseProviders = await User.find({
      role: "provider",
      status: { $in: ["approved", "active"] },
      isApprovedProvider: true,
      isAvailable: true,
      isOnline: true,
    }).select("_id name businessType providerType serviceType lat lng isAvailable isOnline socketId rating phone");

    const matchingProviders = baseProviders.filter((provider) => {
      const providerBusinessType = normalizeBusinessType(
        provider.businessType || provider.providerType || provider.serviceType || ""
      );

      if (!providerBusinessType) return false;
      if (providerBusinessType !== providerType) return false;

      if (requestDoc.targetProviderId) {
        return String(provider._id) === String(requestDoc.targetProviderId);
      }

      return true;
    });

    return matchingProviders;
  }

  async function broadcastRequestToEligibleProviders(ioInstance, requestDoc) {
    const providers = await getEligibleProvidersForBroadcast(requestDoc);
    if (!providers.length) return { count: 0, providerIds: [] };

    const normalizedService = normalizeBusinessType(
      requestDoc.service || requestDoc.providerType
    );

    const payload = {
      requestId: String(requestDoc._id),
      request: requestDoc,
      service: normalizedService,
      providerType: normalizedService,
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
          service: normalizedService,
          requestId: requestDoc._id,
        }),
        requestId: String(requestDoc._id),
        createdAt: new Date().toISOString(),
      });
    }

    return { count: providers.length, providerIds: targetedIds };
  }

  /* =================================================
     AUTH
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
     SOCKET EVENTS
  ================================================= */
  io.on("connection", async (socket) => {
    console.log("⚡ Secure socket connected:", socket.id, "role:", socket.user?.role);

    const myId = String(socket.user?._id || "");
    const myRole = String(socket.user?.role || "").toLowerCase();
    const myBusinessType = normalizeBusinessType(
      socket.user?.businessType || socket.user?.providerType || socket.user?.serviceType || ""
    );

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
          const providerType = normalizeBusinessType(
            provider.businessType || provider.providerType || provider.serviceType || ""
          );

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
        if (!cleanType || !isSupportedBusinessType(cleanType)) {
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

        if (myBusinessType) {
          update.businessType = myBusinessType;
        }

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
          businessType: normalizeBusinessType(
            updated?.businessType || updated?.providerType || updated?.serviceType || ""
          ),
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

        console.log("✅ Joined chat:", requestId);
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
          "accepted",
          "started",
          "arrived",
          "quotation_sent",
          "paid",
          "provider_done",
          "completed",
        ].includes(status);

        const isAdminUser = String(socket.user?.role || "").toLowerCase() === "admin";

        if (!canChat && !isAdminUser) {
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

        const message = await ChatMessage.create({
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

        io.to(roomForChat(requestId)).emit("new_message", { requestId, message });

        const notifyPayload = {
          id: `msg_${message._id}`,
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
            ? {
                $or: [
                  { assignedProviderId: myObjectId },
                  { assignedTo: myObjectId },
                ],
              }
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
              sender: 1,
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
          read: false,
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

  return io;
}