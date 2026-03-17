// models/chat.js
import mongoose from "mongoose";

const { Schema } = mongoose;

const ChatMessageSchema = new Schema(
  {
    // Request the chat belongs to
    requestId: {
      type: Schema.Types.ObjectId,
      ref: "Request",
      required: true,
      index: true,
    },

    // Who sent the message
    sender: {
      type: String,
      enum: ["user", "provider", "admin", "system"],
      required: true,
      lowercase: true,
      trim: true,
      index: true,
    },

    // Sender user ID
    senderId: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: false,
      index: true,
    },

    // Message type
    type: {
      type: String,
      enum: ["text", "voice", "call", "system"],
      default: "text",
      lowercase: true,
      trim: true,
      index: true,
    },

    // Text message or call note text
    text: {
      type: String,
      default: "",
      trim: true,
      maxlength: 2000,
    },

    // Voice note file URL
    audioUrl: {
      type: String,
      default: "",
      trim: true,
    },

    // Voice note duration in seconds
    durationSec: {
      type: Number,
      default: 0,
      min: 0,
    },

    // Extra data (location, attachments, call info, etc)
    meta: {
      type: Schema.Types.Mixed,
      default: {},
    },

    // Users who have read the message
    readBy: [
      {
        type: Schema.Types.ObjectId,
        ref: "User",
        index: true,
      },
    ],
  },
  {
    timestamps: true,
  }
);

/* =================================================
   VALIDATION
================================================= */
ChatMessageSchema.pre("validate", function (next) {
  const type = String(this.type || "text").toLowerCase();
  const text = String(this.text || "").trim();
  const audioUrl = String(this.audioUrl || "").trim();

  if (type === "text" && !text) {
    return next(new Error("Text message must have text"));
  }

  if (type === "voice" && !audioUrl) {
    return next(new Error("Voice message must have audioUrl"));
  }

  if (type === "call") {
    // call logs can have optional text like "Call started" / "Missed call"
  }

  next();
});

/* =================================================
   INDEXES
================================================= */

// Fast chat history lookup
ChatMessageSchema.index({ requestId: 1, createdAt: 1 });

// Fast unread notification lookup
ChatMessageSchema.index({ requestId: 1, readBy: 1 });

// Useful for filtering message type in a request
ChatMessageSchema.index({ requestId: 1, type: 1, createdAt: 1 });

// Prevent model overwrite in dev / hot reload
export default mongoose.models.ChatMessage ||
  mongoose.model("ChatMessage", ChatMessageSchema);
