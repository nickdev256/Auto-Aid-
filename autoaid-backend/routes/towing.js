// ✅ routes/towing.js (UPDATED to use Request model, NOT ServiceRequest)

import express from "express";
import mongoose from "mongoose";
import { v4 as uuid } from "uuid";
import User from "../models/User.js";
import Request from "../models/Request.js";

const router = express.Router();

/* ----------------------------------------------
   Helper: Distance Calculation (Haversine Formula)
---------------------------------------------- */
function calcDistanceKm(lat1, lon1, lat2, lon2) {
  const R = 6371;
  const toRad = (x) => (x * Math.PI) / 180;

  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) *
      Math.cos(toRad(lat2)) *
      Math.sin(dLon / 2) ** 2;

  return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
}

function ensureObjectId(id) {
  return mongoose.Types.ObjectId.isValid(id);
}

function getClient(req) {
  const x = (req.headers["x-client"] || "").toString().toLowerCase();
  return x === "android" ? "android" : "web";
}

/* ----------------------------------------------
   Store towing data in note JSON
---------------------------------------------- */
function makeTowNote({ vehicleInfo, problemDescription, towType }) {
  return JSON.stringify({
    kind: "towing",
    vehicleInfo: vehicleInfo || "",
    problemDescription: problemDescription || "",
    towType: towType || "standard",
  });
}

function parseTowNote(note) {
  try {
    const obj = JSON.parse(note || "{}");
    if (obj && obj.kind === "towing") return obj;
  } catch {}
  return { vehicleInfo: "", problemDescription: "", towType: "standard" };
}

/* ----------------------------------------------
   Standardized Response Mapping (keeps your old UI fields)
---------------------------------------------- */
function mapTowDoc(doc) {
  const o = doc.toObject ? doc.toObject() : doc;

  const meta = parseTowNote(o.note);
  const lat = o.userLocation?.lat ?? 0;
  const lng = o.userLocation?.lng ?? 0;

  return {
    id: o.requestId || o._id, // ✅ keep your existing frontend behavior
    requestId: o.requestId,

    userId: o.userId,
    userName: o.userName,
    userPhone: o.userPhone,

    vehicleInfo: meta.vehicleInfo,
    issue: meta.problemDescription,

    towType: meta.towType || "standard",

    lat,
    lng,

    address: o.problem || "",

    status: o.status,

    // ✅ keep old names even though DB uses assignedProvider*
    assignedTo: o.assignedProviderId || null,
    assignedToName: o.assignedProviderName || "",

    providerLocation: o.providerLocation || null,

    createdAt: o.createdAt,
    updatedAt: o.updatedAt,

    chat: Array.isArray(o.chat) ? o.chat : [],
    requestedFrom: o.requestedFrom || "web",
  };
}

/* ==========================================================
   GET AVAILABLE PENDING REQUESTS
   GET /api/towing/available/:providerId?
========================================================== */
router.get("/available/:providerId?", async (req, res) => {
  try {
    const providerId = req.params.providerId;

    const pending = await Request.find({
      providerType: "towing",
      status: "pending",
      assignedProviderId: null,
    }).sort({ createdAt: -1 });

    // If no provider ID → return all pending
    if (!providerId) return res.json(pending.map(mapTowDoc));

    if (!ensureObjectId(providerId)) return res.json(pending.map(mapTowDoc));

    const provider = await User.findById(providerId);

    if (!provider?.lat || !provider?.lng)
      return res.json(pending.map(mapTowDoc));

    const radiusKm = Number(req.query.radiusKm) || 15;

    const nearby = pending
      .filter((r) => typeof r.userLocation?.lat === "number" && typeof r.userLocation?.lng === "number")
      .map((r) => ({
        doc: r,
        dist: calcDistanceKm(provider.lat, provider.lng, r.userLocation.lat, r.userLocation.lng),
      }))
      .filter((x) => x.dist <= radiusKm)
      .sort((a, b) => a.dist - b.dist)
      .map((x) => mapTowDoc(x.doc));

    return res.json(nearby);
  } catch (err) {
    console.error("Towing available error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   CREATE TOWING REQUEST
   POST /api/towing/request
========================================================== */
router.post("/request", async (req, res) => {
  try {
    const {
      userId,
      userName,
      userPhone = "",
      vehicleInfo = "",
      problemDescription = "",
      towType = "standard",
      lat,
      lng,
    } = req.body;

    if (!userId || !ensureObjectId(userId) || !userName || typeof lat !== "number" || typeof lng !== "number") {
      return res.status(400).json({ message: "Missing fields" });
    }

    // Find nearest provider
    const providers = await User.find({
      role: "provider",
      businessType: "towing",
      status: "approved",
      "subscription.active": true,
      lat: { $exists: true },
      lng: { $exists: true },
    });

    let nearest = null;
    let minDist = Infinity;

    providers.forEach((p) => {
      const d = calcDistanceKm(lat, lng, p.lat, p.lng);
      if (d < minDist) {
        minDist = d;
        nearest = p;
      }
    });

    const requestId = uuid();

    const doc = await Request.create({
      requestId,

      userId: new mongoose.Types.ObjectId(userId),
      userName,
      userPhone,

      providerType: "towing",
      service: "towing",

      userLocation: { lat, lng },

      // store address in "problem" (string field already exists)
      problem: `${lat.toFixed(5)}, ${lng.toFixed(5)}`,

      assignedProviderId: nearest ? nearest._id : null,
      assignedProviderName: nearest ? (nearest.businessName || nearest.name) : "",
      assignedProviderPhone: nearest ? (nearest.phone || "") : "",

      providerLocation: nearest ? { lat: nearest.lat || 0, lng: nearest.lng || 0 } : { lat: 0, lng: 0 },

      status: nearest ? "assigned" : "pending",

      note: makeTowNote({ vehicleInfo, problemDescription, towType }),

      requestedFrom: getClient(req),
      createdByRole: "user",

      chat: [],
    });

    return res.status(201).json(mapTowDoc(doc));
  } catch (err) {
    console.error("Towing request error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   PROVIDER ACCEPTS REQUEST
   POST /api/towing/assign/:id
   (id = requestId uuid)
========================================================== */
router.post("/assign/:id", async (req, res) => {
  try {
    const { providerId } = req.body;

    if (!providerId) return res.status(400).json({ message: "providerId required" });
    if (!ensureObjectId(providerId)) return res.status(400).json({ message: "Invalid providerId" });

    const provider = await User.findById(providerId);
    if (!provider) return res.status(404).json({ message: "Provider not found" });

    if (provider.businessType !== "towing") return res.status(403).json({ message: "Not towing provider" });
    if (!provider.subscription?.active) return res.status(403).json({ message: "Subscription required" });

    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "towing",
    });

    if (!doc) return res.status(404).json({ message: "Request not found" });

    // Prevent race condition
    if (doc.assignedProviderId && String(doc.assignedProviderId) !== String(providerId)) {
      return res.status(409).json({ message: "Already accepted" });
    }

    doc.assignedProviderId = provider._id;
    doc.assignedProviderName = provider.businessName || provider.name;
    doc.assignedProviderPhone = provider.phone || "";
    doc.status = "assigned";
    doc.providerLocation = {
      lat: provider.lat || 0,
      lng: provider.lng || 0,
    };

    await doc.save();
    res.json(mapTowDoc(doc));
  } catch (err) {
    console.error("Towing assign error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   GET LATEST ACTIVE REQUEST FOR USER
   GET /api/towing/latest/:userId
========================================================== */
router.get("/latest/:userId", async (req, res) => {
  try {
    const { userId } = req.params;
    if (!ensureObjectId(userId)) return res.status(400).json({ message: "Invalid userId" });

    const doc = await Request.findOne({
      userId: new mongoose.Types.ObjectId(userId),
      providerType: "towing",
      status: { $in: ["pending", "assigned", "arrived", "in_progress"] },
    }).sort({ createdAt: -1 });

    if (!doc) return res.status(404).json({ message: "No active request" });

    res.json(mapTowDoc(doc));
  } catch (err) {
    console.error("Towing latest error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   GET REQUEST BY ID
   GET /api/towing/:id
   (id = requestId uuid)
========================================================== */
router.get("/:id", async (req, res) => {
  try {
    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "towing",
    });

    if (!doc) return res.status(404).json({ message: "Not found" });

    res.json(mapTowDoc(doc));
  } catch (err) {
    console.error("Towing get error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   GET USER HISTORY
   GET /api/towing/history/:userId
========================================================== */
router.get("/history/:userId", async (req, res) => {
  try {
    const { userId } = req.params;
    if (!ensureObjectId(userId)) return res.status(400).json({ message: "Invalid userId" });

    const list = await Request.find({
      userId: new mongoose.Types.ObjectId(userId),
      providerType: "towing",
    }).sort({ createdAt: -1 });

    res.json(list.map(mapTowDoc));
  } catch (err) {
    console.error("Towing history error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   PROVIDER LOCATION UPDATE
   PATCH /api/towing/update-location
========================================================== */
router.patch("/update-location", async (req, res) => {
  try {
    const { providerId, lat, lng } = req.body;

    if (!providerId || typeof lat !== "number" || typeof lng !== "number") {
      return res.status(400).json({ message: "providerId, lat, lng required" });
    }
    if (!ensureObjectId(providerId)) return res.status(400).json({ message: "Invalid providerId" });

    await Request.updateMany(
      {
        providerType: "towing",
        assignedProviderId: new mongoose.Types.ObjectId(providerId),
        status: { $nin: ["completed", "cancelled"] },
      },
      {
        $set: {
          "providerLocation.lat": lat,
          "providerLocation.lng": lng,
        },
      }
    );

    res.json({ message: "Location updated" });
  } catch (err) {
    console.error("Towing location update error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   UPDATE REQUEST STATUS
   PATCH /api/towing/:id/status
========================================================== */
router.patch("/:id/status", async (req, res) => {
  try {
    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "towing",
    });

    if (!doc) return res.status(404).json({ message: "Not found" });

    if (req.body.status) doc.status = String(req.body.status).trim().toLowerCase();

    await doc.save();
    res.json(mapTowDoc(doc));
  } catch (err) {
    console.error("Towing status update error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   CHAT (SEND)
   POST /api/towing/:id/chat
========================================================== */
router.post("/:id/chat", async (req, res) => {
  try {
    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "towing",
    });

    if (!doc) return res.status(404).json({ message: "Not found" });

    const { sender = "user", text } = req.body;
    if (!text) return res.status(400).json({ message: "text required" });

    if (!Array.isArray(doc.chat)) doc.chat = [];
    doc.chat.push({ sender, text, time: new Date() });

    await doc.save();
    res.json({ messages: doc.chat });
  } catch (err) {
    console.error("Towing chat error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   CHAT (GET)
   GET /api/towing/:id/chat
========================================================== */
router.get("/:id/chat", async (req, res) => {
  try {
    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "towing",
    });

    if (!doc) return res.status(404).json({ message: "Not found" });

    res.json({ messages: Array.isArray(doc.chat) ? doc.chat : [] });
  } catch (err) {
    console.error("Towing chat get error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   PROVIDER ASSIGNED REQUESTS
   GET /api/towing/byProvider/:providerId
========================================================== */
router.get("/byProvider/:providerId", async (req, res) => {
  try {
    const { providerId } = req.params;
    if (!ensureObjectId(providerId)) return res.status(400).json({ message: "Invalid providerId" });

    const list = await Request.find({
      providerType: "towing",
      assignedProviderId: new mongoose.Types.ObjectId(providerId),
    }).sort({ createdAt: -1 });

    res.json(list.map(mapTowDoc));
  } catch (err) {
    console.error("Towing byProvider error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

export default router;