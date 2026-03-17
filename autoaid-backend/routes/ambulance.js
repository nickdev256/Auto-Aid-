// ✅ routes/ambulance.js (UPDATED to use Request model, NOT ServiceRequest)

import express from "express";
import mongoose from "mongoose";
import { v4 as uuid } from "uuid";
import User from "../models/User.js";
import Request from "../models/Request.js";

const router = express.Router();

/** ------------------------------
 * Haversine Distance (km)
--------------------------------*/
function calcDistanceKm(lat1, lon1, lat2, lon2) {
  const R = 6371;
  const toRad = (d) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);

  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) *
      Math.cos(toRad(lat2)) *
      Math.sin(dLon / 2) ** 2;

  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// -----------------------------
// Helper: validate ObjectId
// -----------------------------
function ensureObjectId(id) {
  return mongoose.Types.ObjectId.isValid(id);
}

// ✅ detect platform (Android/Web)
function getClient(req) {
  const x = (req.headers["x-client"] || "").toString().toLowerCase();
  return x === "android" ? "android" : "web";
}

// -----------------------------
// Store ambulance data in note JSON
// -----------------------------
function makeAmbulanceNote({ emergencyType, condition }) {
  return JSON.stringify({
    kind: "ambulance",
    emergencyType: emergencyType || "",
    condition: condition || "",
  });
}

function parseAmbulanceNote(note) {
  try {
    const obj = JSON.parse(note || "{}");
    if (obj && obj.kind === "ambulance") return obj;
  } catch {}
  return { emergencyType: "", condition: "" };
}

// -----------------------------
// Map Request => old response shape (compatible)
// -----------------------------
function mapAmbDoc(doc) {
  const o = doc.toObject ? doc.toObject() : doc;
  const meta = parseAmbulanceNote(o.note);

  const lat = o.userLocation?.lat ?? 0;
  const lng = o.userLocation?.lng ?? 0;

  return {
    requestId: o.requestId || String(o._id),

    userId: o.userId,
    userName: o.userName,
    userPhone: o.userPhone,

    serviceType: "ambulance",
    meta: { emergencyType: meta.emergencyType, condition: meta.condition },

    lat,
    lng,
    address: o.problem || "",

    assignedTo: o.assignedProviderId || null,
    assignedToName: o.assignedProviderName || "",
    providerLocation: o.providerLocation || null,

    status: o.status,
    createdAt: o.createdAt,
    updatedAt: o.updatedAt,

    requestedFrom: o.requestedFrom || "web",
  };
}

/* ============================================================
   1️⃣ CREATE AMBULANCE REQUEST  (AUTO-ASSIGN NEAREST PROVIDER)
============================================================ */
router.post("/request", async (req, res) => {
  try {
    const { userId, userName, phone, emergencyType, condition, lat, lng, address } =
      req.body;

    if (!userId || !ensureObjectId(userId)) {
      return res.status(400).json({ message: "Invalid userId" });
    }
    if (!userName) {
      return res.status(400).json({ message: "userName is required" });
    }
    if (typeof lat !== "number" || typeof lng !== "number") {
      return res.status(400).json({ message: "lat and lng must be numbers" });
    }

    // Get all valid ambulance providers
    const providers = await User.find({
      role: "provider",
      businessType: "ambulance",
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
      userPhone: phone || "",

      providerType: "ambulance",
      service: "ambulance",

      userLocation: { lat, lng },

      // store address string in "problem" field
      problem: address || `${lat.toFixed(5)}, ${lng.toFixed(5)}`,

      // store emergencyType + condition inside note
      note: makeAmbulanceNote({ emergencyType, condition }),

      status: nearest ? "assigned" : "pending",

      assignedProviderId: nearest ? nearest._id : null,
      assignedProviderName: nearest ? nearest.businessName || nearest.name : "",
      assignedProviderPhone: nearest ? nearest.phone || "" : "",
      assignedProviderRating: nearest ? nearest.rating || 0 : 0,

      providerLocation: nearest
        ? { lat: nearest.lat || 0, lng: nearest.lng || 0 }
        : { lat: 0, lng: 0 },

      requestedFrom: getClient(req),
      createdByRole: "user",

      chat: [],
    });

    // keep your old response style
    return res.status(201).json({ id: doc.requestId });
  } catch (err) {
    console.error("Ambulance request error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ============================================================
   2️⃣ PROVIDER VIEW THEIR JOBS (ASSIGNED + UNASSIGNED)
============================================================ */
router.get("/byProvider/:providerId", async (req, res) => {
  try {
    const providerId = req.params.providerId;

    if (!ensureObjectId(providerId)) {
      return res.status(400).json({ message: "Invalid providerId" });
    }

    const provider = await User.findById(providerId);
    if (!provider) return res.status(404).json({ message: "Provider not found" });

    if (provider.businessType !== "ambulance") {
      return res.status(403).json({ message: "Not an ambulance provider" });
    }

    const list = await Request.find({
      providerType: "ambulance",
      $or: [
        { assignedProviderId: new mongoose.Types.ObjectId(providerId) },
        { assignedProviderId: null },
      ],
    }).sort({ createdAt: -1 });

    return res.json(list.map(mapAmbDoc));
  } catch (err) {
    console.error("Ambulance provider view error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ============================================================
   3️⃣ PROVIDER ACCEPT REQUEST
============================================================ */
router.post("/assign/:id", async (req, res) => {
  try {
    const { providerId } = req.body;

    if (!providerId || !ensureObjectId(providerId)) {
      return res.status(400).json({ message: "providerId required" });
    }

    const provider = await User.findById(providerId);
    if (!provider) return res.status(404).json({ message: "Provider not found" });

    if (provider.businessType !== "ambulance") {
      return res.status(403).json({ message: "Not an ambulance provider" });
    }

    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "ambulance",
    });

    if (!doc) return res.status(404).json({ message: "Request not found" });

    doc.assignedProviderId = provider._id;
    doc.assignedProviderName = provider.businessName || provider.name || "";
    doc.assignedProviderPhone = provider.phone || "";
    doc.assignedProviderRating = provider.rating || 0;

    doc.providerLocation = { lat: provider.lat || 0, lng: provider.lng || 0 };
    doc.status = "assigned";

    await doc.save();

    res.json({ message: "Accepted", request: mapAmbDoc(doc) });
  } catch (err) {
    console.error("Ambulance accept error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ============================================================
   UPDATE STATUS
============================================================ */
router.patch("/:id/status", async (req, res) => {
  try {
    const nextStatus = String(req.body.status || "").trim().toLowerCase();
    if (!nextStatus) return res.status(400).json({ message: "status required" });

    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "ambulance",
    });

    if (!doc) return res.status(404).json({ message: "Request not found" });

    doc.status = nextStatus;
    await doc.save();

    return res.json({ message: "Status updated", status: doc.status });
  } catch (err) {
    console.error("Ambulance status update error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ============================================================
   TRACK
============================================================ */
router.get("/track/:id", async (req, res) => {
  try {
    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "ambulance",
    });

    if (!doc) return res.status(404).json({ message: "Request not found" });

    const lat = doc.userLocation?.lat ?? 0;
    const lng = doc.userLocation?.lng ?? 0;

    res.json({
      userLocation: { lat, lng },
      providerLocation: doc.providerLocation || null,
      status: doc.status,
      assignedToName: doc.assignedProviderName || "",
    });
  } catch (err) {
    console.error("Ambulance track error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ============================================================
   USER HISTORY
============================================================ */
router.get("/history/:userId", async (req, res) => {
  try {
    const userId = req.params.userId;

    if (!ensureObjectId(userId)) {
      return res.status(400).json({ message: "Invalid userId" });
    }

    const list = await Request.find({
      userId: new mongoose.Types.ObjectId(userId),
      providerType: "ambulance",
    }).sort({ createdAt: -1 });

    res.json(list.map(mapAmbDoc));
  } catch (err) {
    console.error("Ambulance history error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

export default router;