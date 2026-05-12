import mongoose from "mongoose";

const chatSchema = new mongoose.Schema(
  {
    requestId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Request",
      required: true,
    },

    sender: {
      type: String,
      enum: ["user", "provider", "admin"],
      default: "user",
    },

    senderId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
    },

    type: {
      type: String,
      enum: ["text", "voice", "call"],
      default: "text",
    },

    text: {
      type: String,
      default: "",
    },

    audioUrl: {
      type: String,
      default: "",
    },

    durationSec: {
      type: Number,
      default: 0,
    },

    meta: {
      type: Object,
      default: {},
    },

    readBy: [
      {
        type: mongoose.Schema.Types.ObjectId,
        ref: "User",
      },
    ],
  },
  {
    timestamps: true,
  }
);

const ChatMessage = mongoose.model("ChatMessage", chatSchema);

export default ChatMessage;