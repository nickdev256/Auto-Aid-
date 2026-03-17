// ✅ routes/fuel.js  (UPDATED to use Request model, NOT ServiceRequest)

import express from "express";
import mongoose from "mongoose";
import { v4 as uuid } from "uuid";
import User from "../models/User.js";
import Request from "../models/Request.js";

const router = express.Router();

function ensureObjectId(id) {
  return mongoose.Types.ObjectId.isValid(id);
}

// ✅ NEW: detect platform (android/web) from header
function getClient(req) {
  const x = (req.headers["x-client"] || "").toString().toLowerCase();
  return x === "android" ? "android" : "web";
}

// ✅ store fuel meta safely inside Request.note as JSON string
function makeFuelNote({ fuelType, quantityLitres, paymentMethod }) {
  return JSON.stringify({
    kind: "fuel",
    fuelType: fuelType || "",
    quantityLitres: Number(quantityLitres || 0),
    paymentMethod: paymentMethod || "",
  });
}

function parseFuelNote(note) {
  try {
    const obj = JSON.parse(note || "{}");
    if (obj && obj.kind === "fuel") return obj;
  } catch {}
  return { fuelType: "", quantityLitres: 0, paymentMethod: "" };
}

/**
 * POST /api/fuel/request
 * (kept same endpoint)
 */
router.post("/request", async (req, res) => {
  try {
    const {
      userId,
      userName,
      userPhone,
      fuelType,
      quantityLitres,
      paymentMethod,
      lat,
      lng,
      address,
    } = req.body;

    if (!userId || !ensureObjectId(userId)) {
      return res.status(400).json({ message: "Valid userId is required" });
    }
    if (lat === undefined || lng === undefined) {
      return res.status(400).json({ message: "lat and lng are required" });
    }

    const requestId = uuid(); // ✅ keep old requestId behavior

    const doc = await Request.create({
      // ✅ keep a readable requestId for old routes
      requestId,

      userId: new mongoose.Types.ObjectId(userId),
      userName: userName || "",
      userPhone: userPhone || "",

      providerType: "fuel",
      service: "fuel",

      userLocation: { lat: Number(lat), lng: Number(lng) },
      providerLocation: { lat: 0, lng: 0 },

      status: "pending",

      // ✅ store fuel details in note JSON
      note: makeFuelNote({ fuelType, quantityLitres, paymentMethod }),

      // optional extra text
      problem: address || `${Number(lat).toFixed(5)}, ${Number(lng).toFixed(5)}`,

      // ✅ origin tracking
      requestedFrom: getClient(req),
      createdByRole: "user",

      // ✅ allow chat array if you use it later (safe even if schema doesn't define it)
      chat: [],
    });

    return res.json({ id: doc.requestId }); // ✅ return the uuid like before
  } catch (err) {
    console.error("Fuel request error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * GET /api/fuel/byProvider/:providerId
 */
router.get("/byProvider/:providerId", async (req, res) => {
  try {
    const { providerId } = req.params;

    if (!ensureObjectId(providerId)) {
      return res.status(400).json({ message: "Invalid providerId" });
    }

    const provider = await User.findById(providerId);
    if (!provider) return res.status(404).json({ message: "Provider not found" });

    if (provider.businessType !== "fuel") {
      return res.status(403).json({ message: "Not a fuel provider" });
    }

    const list = await Request.find({
      providerType: "fuel",
      $or: [
        { assignedProviderId: provider._id },
        { assignedProviderId: null },
      ],
    }).sort({ createdAt: -1 });

    // attach parsed fuel meta for frontend convenience
    const data = list.map((r) => ({
      ...r.toObject(),
      meta: parseFuelNote(r.note),
    }));

    return res.json(data);
  } catch (err) {
    console.error("Fuel provider view error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * POST /api/fuel/assign/:id
 * body: { providerId }
 * (id here is requestId uuid, same as before)
 */
router.post("/assign/:id", async (req, res) => {
  try {
    const { providerId } = req.body;
    if (!providerId || !ensureObjectId(providerId)) {
      return res.status(400).json({ message: "Valid providerId is required" });
    }

    const provider = await User.findById(providerId);
    if (!provider) return res.status(404).json({ message: "Provider not found" });
    if (provider.businessType !== "fuel") {
      return res.status(403).json({ message: "Not a fuel provider" });
    }

    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "fuel",
    });
    if (!doc) return res.status(404).json({ message: "Request not found" });

    doc.assignedProviderId = provider._id;
    doc.assignedProviderName = provider.businessName || provider.name || "";
    doc.assignedProviderPhone = provider.phone || "";
    doc.status = "assigned";

    // providerLocation if you have it on provider
    if (provider.lat && provider.lng) {
      doc.providerLocation = { lat: provider.lat, lng: provider.lng };
    }

    await doc.save();

    // socket notify (optional)
    try {
      const io = req.app.get("io");
      if (io) {
        io.to(`track_${doc.requestId}`).emit("fuel:tracking", {
          requestId: doc.requestId,
          providerLocation: doc.providerLocation || null,
          status: doc.status,
          assignedToName: doc.assignedProviderName,
        });
        io.to(`chat_${doc.requestId}`).emit("chat:system", {
          requestId: doc.requestId,
          text: `Provider ${doc.assignedProviderName} assigned.`,
          time: new Date(),
          sender: "system",
        });
      }
    } catch (emitErr) {
      console.warn("Socket emit failed (assign)", emitErr);
    }

    return res.json({
      message: "Assigned",
      request: { ...doc.toObject(), meta: parseFuelNote(doc.note) },
    });
  } catch (err) {
    console.error("Fuel assign error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * PATCH /api/fuel/:id/status
 * body: { status }
 */
router.patch("/:id/status", async (req, res) => {
  try {
    const nextStatus = String(req.body.status || "").trim().toLowerCase();
    if (!nextStatus) return res.status(400).json({ message: "status required" });

    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "fuel",
    });
    if (!doc) return res.status(404).json({ message: "Request not found" });

    doc.status = nextStatus;
    await doc.save();

    try {
      const io = req.app.get("io");
      if (io) {
        io.to(`track_${doc.requestId}`).emit("fuel:status", {
          requestId: doc.requestId,
          status: doc.status,
        });
      }
    } catch (emitErr) {
      console.warn("Socket status emit failed", emitErr);
    }

    return res.json({ message: "Status updated", status: doc.status });
  } catch (err) {
    console.error("Fuel status update error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * POST /api/fuel/:id/provider-location
 */
router.post("/:id/provider-location", async (req, res) => {
  try {
    const { lat, lng } = req.body;

    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "fuel",
    });
    if (!doc) return res.status(404).json({ message: "Request not found" });

    doc.providerLocation = { lat: Number(lat), lng: Number(lng) };
    await doc.save();

    try {
      const io = req.app.get("io");
      if (io) {
        io.to(`track_${doc.requestId}`).emit("fuel:tracking", {
          requestId: doc.requestId,
          providerLocation: doc.providerLocation,
          status: doc.status,
          assignedToName: doc.assignedProviderName,
        });
      }
    } catch (emitErr) {
      console.warn("Socket emit failed (provider-location)", emitErr);
    }

    return res.json({ message: "Provider location updated" });
  } catch (err) {
    console.error("Provider location update error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * GET /api/fuel/track/:id
 */
router.get("/track/:id", async (req, res) => {
  try {
    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "fuel",
    });
    if (!doc) return res.status(404).json({ message: "Request not found" });

    return res.json({
      userLocation: doc.userLocation || { lat: 0, lng: 0 },
      providerLocation: doc.providerLocation || null,
      status: doc.status,
      assignedToName: doc.assignedProviderName || "",
      meta: parseFuelNote(doc.note),
    });
  } catch (err) {
    console.error("Fuel track error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * GET /api/fuel/history/:userId
 */
router.get("/history/:userId", async (req, res) => {
  try {
    const { userId } = req.params;
    if (!ensureObjectId(userId)) {
      return res.status(400).json({ message: "Invalid userId" });
    }

    const list = await Request.find({
      userId: new mongoose.Types.ObjectId(userId),
      providerType: "fuel",
    }).sort({ createdAt: -1 });

    const data = list.map((r) => ({ ...r.toObject(), meta: parseFuelNote(r.note) }));
    return res.json(data);
  } catch (err) {
    console.error("Fuel history error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/**
 * GET /api/fuel/latest/:userId
 */
router.get("/latest/:userId", async (req, res) => {
  try {
    const { userId } = req.params;
    if (!ensureObjectId(userId)) {
      return res.status(400).json({ message: "Invalid userId" });
    }

    const doc = await Request.findOne({
      userId: new mongoose.Types.ObjectId(userId),
      providerType: "fuel",
    }).sort({ createdAt: -1 });

    if (!doc) return res.status(404).json({ message: "No active request" });

    return res.json({ ...doc.toObject(), meta: parseFuelNote(doc.note) });
  } catch (err) {
    console.error("Fuel latest error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

export default router;