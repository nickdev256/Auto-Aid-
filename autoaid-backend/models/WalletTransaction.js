import mongoose from "mongoose";

const WalletTransactionSchema = new mongoose.Schema(
  {
    ownerType: {
      type: String,
      enum: ["user", "provider", "system"],
      required: true,
      lowercase: true,
      trim: true,
      index: true,
    },
    ownerId: {
      type: mongoose.Schema.Types.ObjectId,
      required: true,
      index: true,
    },
    transactionType: {
      type: String,
      enum: [
        "topup",
        "service_payment",
        "provider_credit",
        "system_fee",
        "cash_settlement",
        "refund",
        "payout",
      ],
      required: true,
      lowercase: true,
      trim: true,
      index: true,
    },
    amount: {
      type: Number,
      required: true,
      min: 0,
    },
    direction: {
      type: String,
      enum: ["in", "out"],
      required: true,
      lowercase: true,
      trim: true,
      index: true,
    },
    method: {
      type: String,
      enum: ["airtel_money", "wallet", "cash"],
      required: true,
      lowercase: true,
      trim: true,
      index: true,
    },
    requestId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Request",
      default: null,
      index: true,
    },
    reference: {
      type: String,
      default: "",
      trim: true,
      index: true,
    },
    note: {
      type: String,
      default: "",
      trim: true,
    },
    status: {
      type: String,
      enum: ["pending", "successful", "failed", "reversed"],
      default: "successful",
      lowercase: true,
      trim: true,
      index: true,
    },
  },
  { timestamps: true }
);

/* =================================================
   INDEXES
================================================= */

// Main wallet history queries
WalletTransactionSchema.index({ ownerType: 1, ownerId: 1, createdAt: -1 });

// Filtered owner history
WalletTransactionSchema.index({
  ownerType: 1,
  ownerId: 1,
  status: 1,
  createdAt: -1,
});

// Method-specific owner history
WalletTransactionSchema.index({
  ownerType: 1,
  ownerId: 1,
  method: 1,
  createdAt: -1,
});

// Request-linked transaction lookup
WalletTransactionSchema.index({ requestId: 1, createdAt: -1 });

// Payment verification / reference lookup
WalletTransactionSchema.index({ reference: 1, status: 1 });

// Airtel pending verification
WalletTransactionSchema.index({
  method: 1,
  status: 1,
  requestId: 1,
  createdAt: -1,
});

// Reporting / admin summaries
WalletTransactionSchema.index({
  transactionType: 1,
  status: 1,
  createdAt: -1,
});

// Fast request + method matching
WalletTransactionSchema.index({
  requestId: 1,
  method: 1,
  status: 1,
});

export default mongoose.models.WalletTransaction ||
  mongoose.model("WalletTransaction", WalletTransactionSchema);