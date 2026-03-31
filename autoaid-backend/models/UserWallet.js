import mongoose from "mongoose";

const UserWalletSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      unique: true,
      index: true, // 🔥 fast lookup (critical)
    },

    balance: {
      type: Number,
      default: 0,
      min: 0,
    },

    totalTopUps: {
      type: Number,
      default: 0,
      min: 0,
    },

    totalSpent: {
      type: Number,
      default: 0,
      min: 0,
    },

    totalRefunded: {
      type: Number,
      default: 0,
      min: 0,
    },

    // 🔥 future-ready (optional but powerful)
    currency: {
      type: String,
      default: "UGX",
      uppercase: true,
      trim: true,
    },

    // 🔥 helps detect corruption / inconsistencies
    lastTransactionAt: {
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

// 🔥 MAIN LOOKUP (most important)
UserWalletSchema.index({ userId: 1 });

// 🔥 SORT / ADMIN / ACTIVITY TRACKING
UserWalletSchema.index({ updatedAt: -1 });
UserWalletSchema.index({ balance: -1 });
UserWalletSchema.index({ lastTransactionAt: -1 });

/* =================================================
   SAFE UPDATE HELPER (VERY IMPORTANT)
================================================= */
UserWalletSchema.methods.applyTransaction = function ({
  amount = 0,
  type = "spend", // "topup" | "spend" | "refund"
}) {
  const safeAmount = Number(amount) || 0;

  if (type === "topup") {
    this.balance += safeAmount;
    this.totalTopUps += safeAmount;
  }

  if (type === "spend") {
    this.balance -= safeAmount;
    this.totalSpent += safeAmount;
  }

  if (type === "refund") {
    this.balance += safeAmount;
    this.totalRefunded += safeAmount;
  }

  this.lastTransactionAt = new Date();

  return this;
};

export default mongoose.models.UserWallet ||
  mongoose.model("UserWallet", UserWalletSchema);