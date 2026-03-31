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

const REQUEST_STATUS_ENUM = [
  "pending",
  "accepted",
  "started",
  "arrived",
  "quotation_sent",
  "paid",
  "provider_done",
  "completed",
  "cancelled",
];

const StatusHistorySchema = new Schema(
  {
    status: {
      type: String,
      enum: REQUEST_STATUS_ENUM,
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

    status: {
      type: String,
      enum: REQUEST_STATUS_ENUM,
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

    assignedAt: {
      type: Date,
      default: null,
    },

    tripStartedAt: {
      type: Date,
      default: null,
    },

    arrivedAt: {
      type: Date,
      default: null,
    },

    quoteSentAt: {
      type: Date,
      default: null,
    },

    paymentReadyAt: {
      type: Date,
      default: null,
    },

    providerCompletedAt: {
      type: Date,
      default: null,
    },

    userCompletedAt: {
      type: Date,
      default: null,
    },

    cancelledAt: {
      type: Date,
      default: null,
    },

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

    userLocation: {
      type: LocationSchema,
      default: () => ({ lat: 0, lng: 0 }),
    },

    providerLocation: {
      type: LocationSchema,
      default: () => ({ lat: 0, lng: 0 }),
    },

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

    aiSummary: {
      type: String,
      default: "",
      trim: true,
    },

    aiUrgency: {
      type: String,
      enum: ["", "low", "medium", "high", "critical"],
      default: "",
      lowercase: true,
      trim: true,
    },

    aiSuggestedService: {
      type: String,
      enum: ["", "garage", "towing", "fuel", "ambulance", "none"],
      default: "",
      lowercase: true,
      trim: true,
    },

    aiSafeToDrive: {
      type: String,
      enum: ["", "yes", "no", "unknown"],
      default: "",
      lowercase: true,
      trim: true,
    },

    providerFinalDiagnosis: {
      type: String,
      default: "",
      trim: true,
    },

    providerNotes: {
      type: String,
      default: "",
      trim: true,
    },

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

    providerAmount: {
      type: Number,
      default: 0,
      min: 0,
    },

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

    quotationAccepted: {
      type: Boolean,
      default: false,
      index: true,
    },

    priceSetAt: {
      type: Date,
      default: null,
    },

    pricingStatus: {
      type: String,
      enum: ["not_set", "quoted", "accepted", "approved", "rejected"],
      default: "not_set",
      lowercase: true,
      trim: true,
    },

    paymentStatus: {
      type: String,
      enum: ["unpaid", "pending", "awaiting_cash", "paid", "failed", "refunded"],
      default: "unpaid",
      index: true,
      lowercase: true,
      trim: true,
    },

    paymentMethod: {
      type: String,
      enum: ["", "airtel_money", "cash", "wallet", "mobile_money"],
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

    paymentConfirmedByProvider: {
      type: Boolean,
      default: false,
      index: true,
    },

    paymentConfirmedAt: {
      type: Date,
      default: null,
      index: true,
    },

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

  if (!Array.isArray(this.statusHistory)) this.statusHistory = [];

  if (this.requestPhoto == null) this.requestPhoto = "";
  if (this.requestPhotoPublicId == null) this.requestPhotoPublicId = "";
  if (this.paymentPhoneNumber == null) this.paymentPhoneNumber = "";
  if (this.paymentReference == null) this.paymentReference = "";

  if (this.aiSummary == null) this.aiSummary = "";
  if (this.aiUrgency == null) this.aiUrgency = "";
  if (this.aiSuggestedService == null) this.aiSuggestedService = "";
  if (this.aiSafeToDrive == null) this.aiSafeToDrive = "";
  if (this.providerFinalDiagnosis == null) this.providerFinalDiagnosis = "";
  if (this.providerNotes == null) this.providerNotes = "";

  if (this.paymentConfirmedByProvider == null) this.paymentConfirmedByProvider = false;
  if (this.quotationAccepted == null) this.quotationAccepted = false;

  next();
});

/* =================================================
   STATUS HISTORY TRACKING
================================================= */
RequestSchema.pre("save", function (next) {
  if (!Array.isArray(this.statusHistory)) {
    this.statusHistory = [];
  }

  if (this.isNew) {
    this.statusHistory.push({
      status: this.status || "pending",
      changedAt: new Date(),
      changedBy: this.userId || null,
      note: "Request created",
    });
    return next();
  }

  if (this.isModified("status")) {
    this.statusHistory.push({
      status: this.status,
      changedAt: new Date(),
      changedBy: null,
      note: "Status changed",
    });
  }

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
RequestSchema.index({ aiUrgency: 1, createdAt: -1 });
RequestSchema.index({ aiSuggestedService: 1, createdAt: -1 });
RequestSchema.index({ paymentConfirmedByProvider: 1, paymentStatus: 1, updatedAt: -1 });

export default mongoose.models.Request || mongoose.model("Request", RequestSchema);