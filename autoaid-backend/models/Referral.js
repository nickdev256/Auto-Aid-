import mongoose from "mongoose";

const ReferralSchema = new mongoose.Schema(
  {
    referrerUserId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      index: true,
    },

    referredUserId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      unique: true,
      index: true,
    },

    referralCode: {
      type: String,
      required: true,
      trim: true,
      uppercase: true,
      index: true,
    },

    status: {
      type: String,
      enum: ["signed_up", "discount_applied", "completed", "rewarded"],
      default: "signed_up",
      index: true,
    },

    friendDiscountAmount: {
      type: Number,
      default: 5000,
      min: 0,
    },

    referrerRewardAmount: {
      type: Number,
      default: 5000,
      min: 0,
    },

    qualifyingRequestId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Request",
      default: null,
    },

    rewardedAt: {
      type: Date,
      default: null,
    },
  },
  { timestamps: true }
);

export default mongoose.models.Referral ||
  mongoose.model("Referral", ReferralSchema);