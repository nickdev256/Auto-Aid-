import express from "express";
import mongoose from "mongoose";
import Request from "../models/Request.js";
import ChatMessage from "../models/chat.js";
import { protect } from "../middleware/authMiddleware.js";

const router = express.Router();

async function findRequestByAnyId(reqId, lean = false) {
  const isObjectId = mongoose.Types.ObjectId.isValid(reqId);

  if (isObjectId) {
    return lean ? Request.findById(reqId).lean() : Request.findById(reqId);
  }

  return lean
    ? Request.findOne({ requestId: reqId }).lean()
    : Request.findOne({ requestId: reqId });
}

function canAccessRequest(reqDoc, user) {
  const me = String(user?._id || "");
  const role = String(user?.role || "").toLowerCase();

  if (!reqDoc || !me) return false;
  if (role === "admin") return true;

  const userId = String(reqDoc.userId || "");
  const providerId = String(reqDoc.assignedProviderId || reqDoc.assignedTo || "");

  return me === userId || (providerId && me === providerId);
}

/**
 * GET /api/chat/:requestId
 */
router.get("/:requestId", protect, async (req, res) => {
  try {
    const reqId = req.params.requestId;
    const request = await findRequestByAnyId(reqId, true);

    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!canAccessRequest(request, req.user)) {
      return res.status(403).json({ message: "Not allowed" });
    }

    const requestObjectId = mongoose.Types.ObjectId.isValid(String(request._id))
      ? new mongoose.Types.ObjectId(String(request._id))
      : null;

    if (!requestObjectId) {
      return res.status(400).json({ message: "Invalid request id" });
    }

    const messages = await ChatMessage.find({ requestId: requestObjectId })
      .sort({ createdAt: 1 })
      .limit(200);

    return res.json(messages);
  } catch (err) {
    console.error("LOAD CHAT ERROR:", err);
    return res.status(500).json({ message: "Failed to load messages" });
  }
});

/**
 * POST /api/chat/:requestId
 */
router.post("/:requestId", protect, async (req, res) => {
  try {
    const { text, meta } = req.body;
    const reqId = req.params.requestId;

    if (!text || !String(text).trim()) {
      return res.status(400).json({ message: "Message text is required" });
    }

    const request = await findRequestByAnyId(reqId, false);

    if (!request) {
      return res.status(404).json({ message: "Request not found" });
    }

    if (!canAccessRequest(request, req.user)) {
      return res.status(403).json({ message: "Not allowed" });
    }

    const clean = String(text).trim();
    const role = String(req.user?.role || "user").toLowerCase();
    const sender =
      role === "provider"
        ? "provider"
        : role === "admin"
        ? "admin"
        : "user";

    const msg = await ChatMessage.create({
      requestId: request._id,
      sender,
      senderId: req.user?._id || undefined,
      text: clean,
      meta: meta || {},
      readBy: req.user?._id ? [req.user._id] : [],
    });

    request.lastMessage = {
      text: clean,
      sender,
      createdAt: msg.createdAt || new Date(),
    };

    await request.save();

    return res.json(msg);
  } catch (err) {
    console.error("SEND CHAT ERROR:", err);
    return res.status(500).json({ message: "Failed to send message" });
  }
});

export default router;