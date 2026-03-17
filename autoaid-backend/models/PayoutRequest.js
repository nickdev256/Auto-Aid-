import mongoose from "mongoose";

const PayoutRequestSchema = new mongoose.Schema(
  {
    providerId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    amount: {
      type: Number,
      required: true,
      min: 1,
    },
    method: {
      type: String,
      enum: ["mobile_money", "bank"],
      required: true,
    },
    accountName: { type: String, default: "" },
    phoneNumber: { type: String, default: "" },
    bankName: { type: String, default: "" },
    accountNumber: { type: String, default: "" },
    status: {
      type: String,
      enum: ["pending", "approved", "rejected", "paid"],
      default: "pending",
    },
    adminNote: { type: String, default: "" },
    paidAt: { type: Date, default: null },
  },
  { timestamps: true }
);

export default mongoose.model("PayoutRequest", PayoutRequestSchema);