import mongoose from "mongoose";

const PayoutRequestSchema = new mongoose.Schema(
  {
    providerId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      index: true,
    },
    amount: {
      type: Number,
      required: true,
      min: 1,
    },
    method: {
      type: String,
      enum: ["airtel_money"],
      default: "airtel_money",
      required: true,
      lowercase: true,
      trim: true,
    },
    accountName: {
      type: String,
      required: true,
      trim: true,
    },
    phoneNumber: {
      type: String,
      required: true,
      trim: true,
    },
    status: {
      type: String,
      enum: ["pending", "approved", "rejected", "paid"],
      default: "pending",
      index: true,
      lowercase: true,
      trim: true,
    },
    adminNote: {
      type: String,
      default: "",
      trim: true,
    },
    paidAt: {
      type: Date,
      default: null,
      index: true,
    },
  },
  { timestamps: true }
);

/* =================================================
   INDEXES
================================================= */
PayoutRequestSchema.index({ providerId: 1, status: 1, createdAt: -1 });
PayoutRequestSchema.index({ status: 1, createdAt: -1 });
PayoutRequestSchema.index({ providerId: 1, createdAt: -1 });
PayoutRequestSchema.index({ providerId: 1, paidAt: -1 });

export default mongoose.models.PayoutRequest ||
  mongoose.model("PayoutRequest", PayoutRequestSchema);