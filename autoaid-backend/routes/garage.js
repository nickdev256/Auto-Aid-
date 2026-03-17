// ✅ routes/garage.js (UPDATED to use Request model, NOT ServiceRequest)

import express from "express";
import mongoose from "mongoose";
import User from "../models/User.js";
import Request from "../models/Request.js";
import { v4 as uuid } from "uuid";

const router = express.Router();

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
// Store garage data in note JSON
// -----------------------------
function makeGarageNote({ vehicleInfo, issue }) {
  return JSON.stringify({
    kind: "garage",
    vehicleInfo: vehicleInfo || "",
    issue: issue || "",
  });
}

function parseGarageNote(note) {
  try {
    const obj = JSON.parse(note || "{}");
    if (obj && obj.kind === "garage") return obj;
  } catch {}
  return { vehicleInfo: "", issue: "" };
}

// -----------------------------
// Map Request => old UI format
// -----------------------------
function mapReq(doc) {
  const o = doc.toObject ? doc.toObject() : doc;
  const meta = parseGarageNote(o.note);

  const lat = o.userLocation?.lat ?? 0;
  const lng = o.userLocation?.lng ?? 0;

  return {
    requestId: o.requestId || String(o._id),
    userId: o.userId,
    userName: o.userName,
    userPhone: o.userPhone,

    serviceType: "garage", // keep old field
    issue: meta.issue,
    vehicleInfo: meta.vehicleInfo,

    lat,
    lng,
    address: o.problem || "",

    assignedTo: o.assignedProviderId || null,
    assignedToName: o.assignedProviderName || "",
    providerLocation: o.providerLocation || null,

    status: o.status,
    createdAt: o.createdAt,
    requestedFrom: o.requestedFrom || "web",
  };
}

/* ==========================================================
   CREATE GARAGE REQUEST
   POST /api/garage/request
========================================================== */
router.post("/request", async (req, res) => {
  try {
    const { userId, userName, userPhone, vehicleInfo, issue, lat, lng, address } = req.body;

    if (!userId || !ensureObjectId(userId)) {
      return res.status(400).json({ message: "Invalid userId" });
    }
    if (!userName) {
      return res.status(400).json({ message: "userName is required" });
    }
    if (typeof lat !== "number" || typeof lng !== "number") {
      return res.status(400).json({ message: "lat and lng must be numbers" });
    }

    const requestId = uuid();

    const doc = await Request.create({
      requestId,

      userId: new mongoose.Types.ObjectId(userId),
      userName,
      userPhone: userPhone || "",

      providerType: "garage",
      service: "garage",

      userLocation: { lat, lng },

      // store address string in "problem" field
      problem: address || `${lat.toFixed(5)}, ${lng.toFixed(5)}`,

      status: "pending",

      note: makeGarageNote({ vehicleInfo, issue }),

      requestedFrom: getClient(req),
      createdByRole: "user",

      chat: [],
    });

    res.status(201).json({ message: "Request created", requestId: doc.requestId });
  } catch (err) {
    console.error("Garage request create error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   GET PENDING GARAGE REQUESTS
   GET /api/garage/available
========================================================== */
router.get("/available", async (req, res) => {
  try {
    const docs = await Request.find({
      providerType: "garage",
      status: "pending",
    }).sort({ createdAt: -1 });

    res.json(docs.map(mapReq));
  } catch (err) {
    console.error("Garage available error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   GET ASSIGNED REQUESTS FOR PROVIDER
   GET /api/garage/byProvider/:providerId
========================================================== */
router.get("/byProvider/:providerId", async (req, res) => {
  try {
    const { providerId } = req.params;
    if (!ensureObjectId(providerId)) {
      return res.status(400).json({ message: "Invalid providerId" });
    }

    const docs = await Request.find({
      providerType: "garage",
      assignedProviderId: new mongoose.Types.ObjectId(providerId),
    }).sort({ createdAt: -1 });

    res.json(docs.map(mapReq));
  } catch (err) {
    console.error("garage byProvider error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   GET LATEST GARAGE REQUEST FOR A USER
   GET /api/garage/latest/:userId
========================================================== */
router.get("/latest/:userId", async (req, res) => {
  try {
    const { userId } = req.params;
    if (!ensureObjectId(userId)) {
      return res.status(400).json({ message: "Invalid userId" });
    }

    const doc = await Request.findOne({
      providerType: "garage",
      userId: new mongoose.Types.ObjectId(userId),
    }).sort({ createdAt: -1 });

    if (!doc) return res.status(404).json({ message: "No request found" });

    res.json(mapReq(doc));
  } catch (err) {
    console.error("Garage latest request error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   GET REQUEST BY requestId (uuid)
   GET /api/garage/:id
========================================================== */
router.get("/:id", async (req, res) => {
  try {
    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "garage",
    });

    if (!doc) return res.status(404).json({ message: "Request not found" });

    res.json(mapReq(doc));
  } catch (err) {
    console.error("Garage get by id error:", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   GET USER HISTORY
   GET /api/garage/history/:userId
========================================================== */
router.get("/history/:userId", async (req, res) => {
  try {
    const { userId } = req.params;
    if (!ensureObjectId(userId)) {
      return res.status(400).json({ message: "Invalid userId" });
    }

    const list = await Request.find({
      userId: new mongoose.Types.ObjectId(userId),
      providerType: "garage",
    }).sort({ createdAt: -1 });

    // keep your original response (raw docs) OR mapped (better)
    return res.json(list.map(mapReq));
  } catch (err) {
    console.error("Garage history error:", err);
    return res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   ASSIGN PROVIDER TO REQUEST
   POST /api/garage/:id/assign
   body: { providerId }
========================================================== */
router.post("/:id/assign", async (req, res) => {
  try {
    const { providerId } = req.body;

    if (!providerId || !ensureObjectId(providerId)) {
      return res.status(400).json({ message: "providerId required" });
    }

    const provider = await User.findById(providerId);
    if (!provider) return res.status(404).json({ message: "Provider not found" });

    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "garage",
    });

    if (!doc) return res.status(404).json({ message: "Request not found" });

    doc.assignedProviderId = provider._id;
    doc.assignedProviderName = provider.businessName || provider.name || "";
    doc.assignedProviderPhone = provider.phone || "";
    doc.status = "assigned";

    // optional: set providerLocation from provider profile
    doc.providerLocation = {
      lat: provider.lat || 0,
      lng: provider.lng || 0,
    };

    await doc.save();

    res.json(mapReq(doc));
  } catch (err) {
    console.error("garage assign error", err);
    res.status(500).json({ message: "Server error" });
  }
});

/* ==========================================================
   UPDATE REQUEST STATUS
   PATCH /api/garage/:id/status
   body: { status }
========================================================== */
router.patch("/:id/status", async (req, res) => {
  try {
    const nextStatus = String(req.body.status || "").trim().toLowerCase();
    if (!nextStatus) return res.status(400).json({ message: "status required" });

    const doc = await Request.findOne({
      requestId: req.params.id,
      providerType: "garage",
    });

    if (!doc) return res.status(404).json({ message: "Request not found" });

    doc.status = nextStatus;
    await doc.save();

    res.json(mapReq(doc));
  } catch (err) {
    console.error("garage status update", err);
    res.status(500).json({ message: "Server error" });
  }
});

export default router;