import mongoose from "mongoose";

const { Schema } = mongoose;

/* =================================================
   SUB SCHEMAS
================================================= */
const LocationSchema = new Schema(
  {
    lat: { type: Number, default: 0 },
    lng: { type: Number, default: 0 },
  },
  { _id: false }
);

const LastMessageSchema = new Schema(
  {
    text: { type: String, default: "" },
    sender: {
      type: String,
      enum: ["user", "provider", "admin", "system"],
      default: "user",
      lowercase: true,
      trim: true,
    },
    createdAt: { type: Date, default: null },
  },
  { _id: false }
);

const StatusHistorySchema = new Schema(
  {
    status: {
      type: String,
      enum: [
        "pending",
        "assigned",
        "arrived",
        "quoted",
        "awaiting_payment",
        "in_progress",
        "awaiting_dual_confirmation",
        "completed",
        "cancelled",
      ],
      required: true,
      lowercase: true,
      trim: true,
    },
    changedAt: {
      type: Date,
      default: Date.now,
    },
    changedBy: {
      type: Schema.Types.ObjectId,
      ref: "User",
      default: null,
    },
    note: {
      type: String,
      default: "",
      trim: true,
    },
  },
  { _id: false }
);

/* =================================================
   MAIN REQUEST SCHEMA
================================================= */
const RequestSchema = new Schema(
  {
    // -------------------------
    // USER
    // -------------------------
    userId: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: true,
      index: true,
    },
    userName: {
      type: String,
      default: "",
      trim: true,
    },
    userPhone: {
      type: String,
      default: "",
      trim: true,
    },

    // -------------------------
    // SOURCE
    // -------------------------
    requestedFrom: {
      type: String,
      enum: ["android", "web"],
      default: "web",
      index: true,
    },

    createdByRole: {
      type: String,
      enum: ["user", "provider", "admin"],
      default: "user",
      index: true,
    },

    // -------------------------
    // TYPE
    // -------------------------
    providerType: {
      type: String,
      enum: ["towing", "fuel", "ambulance", "garage"],
      required: true,
      index: true,
      lowercase: true,
      trim: true,
    },

    service: {
      type: String,
      enum: ["towing", "fuel", "ambulance", "garage"],
      required: true,
      lowercase: true,
      trim: true,
    },

    // -------------------------
    // TARGETING
    // -------------------------
    targetProviderId: {
      type: Schema.Types.ObjectId,
      ref: "User",
      default: null,
      index: true,
    },

    declinedBy: {
      type: [Schema.Types.ObjectId],
      ref: "User",
      default: [],
      index: true,
    },

    // -------------------------
    // STATUS / ASSIGNMENT
    // -------------------------
    status: {
      type: String,
      enum: [
        "pending",
        "assigned",
        "arrived",
        "quoted",
        "awaiting_payment",
        "in_progress",
        "awaiting_dual_confirmation",
        "completed",
        "cancelled",
      ],
      default: "pending",
      index: true,
      lowercase: true,
      trim: true,
    },

    statusHistory: {
      type: [StatusHistorySchema],
      default: [],
    },

    assignedProviderId: {
      type: Schema.Types.ObjectId,
      ref: "User",
      default: null,
      index: true,
    },

    // ✅ legacy compatibility
    assignedTo: {
      type: Schema.Types.ObjectId,
      ref: "User",
      default: null,
      index: true,
    },

    assignedProviderName: {
      type: String,
      default: "",
      trim: true,
    },
    assignedProviderPhone: {
      type: String,
      default: "",
      trim: true,
    },
    assignedProviderRating: {
      type: Number,
      default: 0,
      min: 0,
    },

    // -------------------------
    // COMPLETION CONFIRMATION
    // -------------------------
    providerCompleted: {
      type: Boolean,
      default: false,
      index: true,
    },

    userCompleted: {
      type: Boolean,
      default: false,
      index: true,
    },

    completedAt: {
      type: Date,
      default: null,
      index: true,
    },

    // -------------------------
    // LOCATION
    // -------------------------
    userLocation: {
      type: LocationSchema,
      default: () => ({ lat: 0, lng: 0 }),
    },

    providerLocation: {
      type: LocationSchema,
      default: () => ({ lat: 0, lng: 0 }),
    },

    // -------------------------
    // DETAILS
    // -------------------------
    vehicleInfo: {
      type: String,
      default: "",
      trim: true,
    },

    problem: {
      type: String,
      default: "",
      trim: true,
    },

    urgency: {
      type: String,
      enum: ["normal", "urgent"],
      default: "normal",
      lowercase: true,
      trim: true,
    },

    towType: {
      type: String,
      default: "",
      trim: true,
    },

    note: {
      type: String,
      default: "",
      trim: true,
    },

    // -------------------------
    // OPTIONAL PHOTO ON REQUEST
    // -------------------------
    requestPhoto: {
      type: String,
      default: "",
      trim: true,
    },

    requestPhotoPublicId: {
      type: String,
      default: "",
      trim: true,
    },

    // -------------------------
    // PRICING
    // -------------------------
    providerAmount: {
      type: Number,
      default: 0,
      min: 0,
    },

    // ✅ extra pricing compatibility fields
    quotedAmount: {
      type: Number,
      default: 0,
      min: 0,
    },

    quoteAmount: {
      type: Number,
      default: 0,
      min: 0,
    },

    agreedAmount: {
      type: Number,
      default: 0,
      min: 0,
    },

    finalAmount: {
      type: Number,
      default: 0,
      min: 0,
    },

    amount: {
      type: Number,
      default: 0,
      min: 0,
    },

    price: {
      type: Number,
      default: 0,
      min: 0,
    },

    systemFee: {
      type: Number,
      default: 0,
      min: 0,
    },

    totalAmount: {
      type: Number,
      default: 0,
      min: 0,
    },

    priceSetByProvider: {
      type: Boolean,
      default: false,
    },

    priceSetAt: {
      type: Date,
      default: null,
    },

    pricingStatus: {
      type: String,
      enum: ["not_set", "quoted", "approved", "rejected"],
      default: "not_set",
      lowercase: true,
      trim: true,
    },

    // -------------------------
    // PAYMENT
    // -------------------------
    paymentStatus: {
      type: String,
      enum: [
        "unpaid",
        "paid",
        "held_in_escrow",
        "released",
        "refunded",
      ],
      default: "unpaid",
      index: true,
      lowercase: true,
      trim: true,
    },

    paymentMethod: {
      type: String,
      enum: [
        "",
        "mobile_money",
        "momo",
        "mtn",
        "airtel",
        "card",
        "cash",
        "wallet",
      ],
      default: "",
      lowercase: true,
      trim: true,
    },

    paymentAmount: {
      type: Number,
      default: 0,
      min: 0,
    },

    paymentPhoneNumber: {
      type: String,
      default: "",
      trim: true,
    },

    paymentReference: {
      type: String,
      default: "",
      trim: true,
    },

    paidAt: {
      type: Date,
      default: null,
    },

    // -------------------------
    // CHAT SUMMARY
    // -------------------------
    lastMessage: {
      type: LastMessageSchema,
      default: () => ({
        text: "",
        sender: "user",
        createdAt: null,
      }),
    },
  },
  { timestamps: true }
);

/* =================================================
   PRE-VALIDATE
================================================= */
RequestSchema.pre("validate", function (next) {
  if (this.targetProviderId === "") this.targetProviderId = null;
  if (this.assignedProviderId === "") this.assignedProviderId = null;
  if (this.assignedTo === "") this.assignedTo = null;

  if (!Array.isArray(this.statusHistory)) {
    this.statusHistory = [];
  }

  if (this.requestPhoto == null) this.requestPhoto = "";
  if (this.requestPhotoPublicId == null) this.requestPhotoPublicId = "";
  if (this.paymentPhoneNumber == null) this.paymentPhoneNumber = "";

  next();
});

/* =================================================
   INDEXES
================================================= */
RequestSchema.index({ providerType: 1, status: 1, createdAt: -1 });
RequestSchema.index({ assignedProviderId: 1, status: 1, updatedAt: -1 });
RequestSchema.index({ assignedTo: 1, status: 1, updatedAt: -1 });
RequestSchema.index({ userId: 1, createdAt: -1 });
RequestSchema.index({ pricingStatus: 1, paymentStatus: 1, updatedAt: -1 });
RequestSchema.index({ targetProviderId: 1, status: 1, createdAt: -1 });
RequestSchema.index({ providerCompleted: 1, userCompleted: 1, status: 1 });
RequestSchema.index({ completedAt: -1 });

export default mongoose.models.Request || mongoose.model("Request", RequestSchema);