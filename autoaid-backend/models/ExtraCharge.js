import mongoose from "mongoose";

const { Schema } = mongoose;

const ExtraChargeSchema = new Schema(
  {
    requestId: {
      type: Schema.Types.ObjectId,
      ref: "Request",
      required: true,
      index: true,
    },
    providerId: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: true,
      index: true,
    },
    amount: {
      type: Number,
      required: true,
      min: 0,
      default: 0,
    },
    reason: {
      type: String,
      default: "",
      trim: true,
    },
    note: {
      type: String,
      default: "",
      trim: true,
    },
    status: {
      type: String,
      enum: ["pending", "approved", "rejected", "paid"],
      default: "pending",
      lowercase: true,
      trim: true,
      index: true,
    },
    paymentMethod: {
      type: String,
      enum: ["", "airtel_money", "cash", "wallet", "mobile_money"],
      default: "",
      lowercase: true,
      trim: true,
    },
    paidAt: {
      type: Date,
      default: null,
    },
  },
  { timestamps: true }
);

ExtraChargeSchema.index({ requestId: 1, providerId: 1, createdAt: -1 });

export default mongoose.models.ExtraCharge ||
  mongoose.model("ExtraCharge", ExtraChargeSchema);