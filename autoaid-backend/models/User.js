import mongoose from "mongoose";
import bcrypt from "bcryptjs";
import { encrypt, decrypt } from "../utils/crypto.js";

const { Schema } = mongoose;

/* =================================================
   SUBSCRIPTION SCHEMA
================================================= */
const SubscriptionSchema = new Schema(
  {
    plan: {
      type: String,
      enum: ["free", "monthly", "quarterly", "yearly", "premium"],
      default: "free",
    },
    active: { type: Boolean, default: false },
    startDate: { type: Date, default: null },
    expiryDate: { type: Date, default: null },
    paymentMethod: { type: String, default: null },
    price: { type: Number, default: 0, min: 0 },
  },
  { _id: false }
);

/* =================================================
   PAYOUT INFO SCHEMA
================================================= */
const PayoutInfoSchema = new Schema(
  {
    method: {
      type: String,
      enum: ["mobile_money", "bank"],
      default: "mobile_money",
    },
    accountName: { type: String, default: "" },
    phoneNumber: { type: String, default: "" },
    bankName: { type: String, default: "" },
    accountNumber: { type: String, default: "" },
    isVerified: { type: Boolean, default: false },
  },
  { _id: false }
);

/* =================================================
   WALLET SCHEMA
================================================= */
const WalletSchema = new Schema(
  {
    totalEarned: { type: Number, default: 0, min: 0 },
    availableBalance: { type: Number, default: 0, min: 0 },
    pendingBalance: { type: Number, default: 0, min: 0 },
    totalPaidOut: { type: Number, default: 0, min: 0 },
  },
  { _id: false }
);

/* =================================================
   USER SCHEMA
================================================= */
const UserSchema = new Schema(
  {
    name: {
      type: String,
      required: true,
      trim: true,
    },

    email: {
      type: String,
      required: true,
      unique: true,
      lowercase: true,
      trim: true,
      index: true,
    },

    password: {
      type: String,
      required: true,
    },

    phone: {
      type: String,
      default: "",
    },

    role: {
      type: String,
      enum: ["user", "provider", "admin"],
      default: "user",
      index: true,
    },

    status: {
      type: String,
      enum: ["active", "inactive", "pending", "approved", "rejected"],
      default: "active",
      index: true,
    },

    verificationStatus: {
      type: String,
      enum: ["not_verified", "pending", "verified", "rejected"],
      default: "not_verified",
      index: true,
    },

    // Normalized backend values only
    verificationDocumentType: {
      type: String,
      enum: ["", "national_id", "passport", "drivers_license"],
      default: "",
    },

    verificationDocumentUrl: {
      type: String,
      default: "",
    },

    verificationSubmittedAt: {
      type: Date,
      default: null,
    },

    verificationReviewedAt: {
      type: Date,
      default: null,
    },

    verificationRejectionReason: {
      type: String,
      default: "",
    },

    /* =================================================
       PROVIDER VERIFICATION FILES
    ================================================= */
    workLicenseDocumentUrl: {
      type: String,
      default: "",
    },

    businessRegistrationDocumentUrl: {
      type: String,
      default: "",
    },

    nationalIdFrontUrl: {
      type: String,
      default: "",
    },

    nationalIdBackUrl: {
      type: String,
      default: "",
    },

    profileImage: {
      type: String,
      default: "",
    },

    registeredFrom: {
      type: String,
      enum: ["android", "web"],
      default: "web",
    },

    lastLoginFrom: {
      type: String,
      enum: ["android", "web", null],
      default: null,
    },

    businessName: {
      type: String,
      default: "",
    },

    businessType: {
      type: String,
      enum: ["", "garage", "towing", "fuel", "ambulance"],
      default: "",
      lowercase: true,
      trim: true,
      index: true,
    },

    servicesOffered: {
      type: [String],
      enum: ["garage", "towing", "fuel", "ambulance"],
      default: [],
    },

    address: {
      type: String,
      default: "",
    },

    lat: {
      type: Number,
      default: 0,
      index: true,
    },

    lng: {
      type: Number,
      default: 0,
      index: true,
    },

    rating: {
      type: Number,
      default: 0,
      min: 0,
      max: 5,
    },

    isApprovedProvider: {
      type: Boolean,
      default: false,
      index: true,
    },

    isAvailable: {
      type: Boolean,
      default: false,
      index: true,
    },

    isOnline: {
      type: Boolean,
      default: false,
      index: true,
    },

    socketId: {
      type: String,
      default: "",
      index: true,
    },

    lastSeenAt: {
      type: Date,
      default: null,
    },

    currentRequestId: {
      type: Schema.Types.ObjectId,
      ref: "Request",
      default: null,
    },

    subscription: {
      type: SubscriptionSchema,
      default: () => ({
        plan: "free",
        active: false,
        startDate: null,
        expiryDate: null,
        paymentMethod: null,
        price: 0,
      }),
    },

    payoutInfo: {
      type: PayoutInfoSchema,
      default: () => ({
        method: "mobile_money",
        accountName: "",
        phoneNumber: "",
        bankName: "",
        accountNumber: "",
        isVerified: false,
      }),
    },

    wallet: {
      type: WalletSchema,
      default: () => ({
        totalEarned: 0,
        availableBalance: 0,
        pendingBalance: 0,
        totalPaidOut: 0,
      }),
    },
  },
  { timestamps: true }
);

/* =================================================
   NORMALIZE BEFORE VALIDATE
================================================= */
UserSchema.pre("validate", function (next) {
  if (!Array.isArray(this.servicesOffered)) {
    this.servicesOffered = [];
  }

  // Normalize document type aliases
  const docMap = {
    "National ID": "national_id",
    "national id": "national_id",
    national_id: "national_id",
    Passport: "passport",
    passport: "passport",
    "Driver's License": "drivers_license",
    "drivers license": "drivers_license",
    drivers_license: "drivers_license",
  };

  if (this.verificationDocumentType) {
    this.verificationDocumentType =
      docMap[this.verificationDocumentType] || this.verificationDocumentType;
  }

  if (this.role !== "provider") {
    this.businessType = "";
    this.servicesOffered = [];
    this.isApprovedProvider = false;
    this.isAvailable = false;
    this.currentRequestId = null;
  }

  if (this.status === "approved" && this.role === "provider") {
    this.isApprovedProvider = true;
  }

  if (this.status === "pending" || this.status === "rejected") {
    this.isApprovedProvider = false;
  }

  next();
});

/* =================================================
   FIELD ENCRYPTION
================================================= */
UserSchema.pre("save", function (next) {
  if (this.isModified("phone") && this.phone) {
    this.phone = encrypt(this.phone);
  }

  if (this.isModified("address") && this.address) {
    this.address = encrypt(this.address);
  }

  if (this.isModified("businessName") && this.businessName) {
    this.businessName = encrypt(this.businessName);
  }

  if (this.isModified("payoutInfo.phoneNumber") && this.payoutInfo?.phoneNumber) {
    this.payoutInfo.phoneNumber = encrypt(this.payoutInfo.phoneNumber);
  }

  if (this.isModified("payoutInfo.accountName") && this.payoutInfo?.accountName) {
    this.payoutInfo.accountName = encrypt(this.payoutInfo.accountName);
  }

  if (this.isModified("payoutInfo.bankName") && this.payoutInfo?.bankName) {
    this.payoutInfo.bankName = encrypt(this.payoutInfo.bankName);
  }

  if (this.isModified("payoutInfo.accountNumber") && this.payoutInfo?.accountNumber) {
    this.payoutInfo.accountNumber = encrypt(this.payoutInfo.accountNumber);
  }

  next();
});

/* =================================================
   PASSWORD HASH
================================================= */
UserSchema.pre("save", async function (next) {
  if (!this.isModified("password")) return next();
  this.password = await bcrypt.hash(this.password, 10);
  next();
});

/* =================================================
   METHODS
================================================= */
UserSchema.methods.comparePassword = async function (password) {
  return bcrypt.compare(password, this.password);
};

UserSchema.methods.getDecrypted = function () {
  const obj = this.toObject();

  if (obj.phone) obj.phone = decrypt(obj.phone);
  if (obj.address) obj.address = decrypt(obj.address);
  if (obj.businessName) obj.businessName = decrypt(obj.businessName);

  if (obj.payoutInfo?.phoneNumber) {
    obj.payoutInfo.phoneNumber = decrypt(obj.payoutInfo.phoneNumber);
  }
  if (obj.payoutInfo?.accountName) {
    obj.payoutInfo.accountName = decrypt(obj.payoutInfo.accountName);
  }
  if (obj.payoutInfo?.bankName) {
    obj.payoutInfo.bankName = decrypt(obj.payoutInfo.bankName);
  }
  if (obj.payoutInfo?.accountNumber) {
    obj.payoutInfo.accountNumber = decrypt(obj.payoutInfo.accountNumber);
  }

  return obj;
};

UserSchema.index({ role: 1, status: 1 });
UserSchema.index({
  role: 1,
  businessType: 1,
  isApprovedProvider: 1,
  isAvailable: 1,
  isOnline: 1,
});
UserSchema.index({ businessType: 1, lat: 1, lng: 1 });
UserSchema.index({ verificationStatus: 1 });

export default mongoose.models.User || mongoose.model("User", UserSchema);