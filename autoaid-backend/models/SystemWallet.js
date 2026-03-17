import mongoose from "mongoose";

const SystemWalletSchema = new mongoose.Schema(
  {
    totalSystemFees: {
      type: Number,
      default: 0,
    },
    totalSubscriptionRevenue: {
      type: Number,
      default: 0,
    },
    totalRevenue: {
      type: Number,
      default: 0,
    },
  },
  { timestamps: true }
);

export default mongoose.models.SystemWallet ||
  mongoose.model("SystemWallet", SystemWalletSchema);