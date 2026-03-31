import mongoose from "mongoose";

const ProviderSubscriptionSchema = new mongoose.Schema(
  {
    providerId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      unique: true,
      index: true,
    },
    planId: {
      type: String,
      enum: ["monthly", "quarterly", "yearly"],
      required: true,
    },
    planName: {
      type: String,
      required: true,
    },
    amount: {
      type: Number,
      required: true,
      min: 0,
    },
    durationDays: {
      type: Number,
      required: true,
      min: 1,
    },
    active: {
      type: Boolean,
      default: false,
      index: true,
    },
    paymentStatus: {
      type: String,
      enum: ["pending", "paid", "failed"],
      default: "pending",
    },
    paymentMethod: {
      type: String,
      default: "mobile_money",
    },
    phoneNumber: {
      type: String,
      default: "",
    },
    network: {
      type: String,
      enum: ["mtn", "airtel", ""],
      default: "",
    },
    startedAt: {
      type: Date,
      default: null,
    },
    expiryDate: {
      type: Date,
      default: null,
      index: true,
    },
    lastPaymentAt: {
      type: Date,
      default: null,
    },
    paymentReference: {
      type: String,
      default: "",
    },
  },
  { timestamps: true }
);

export default mongoose.models.ProviderSubscription ||
  mongoose.model("ProviderSubscription", ProviderSubscriptionSchema);