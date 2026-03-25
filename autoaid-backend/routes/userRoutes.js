import mongoose from "mongoose";
import bcrypt from "bcryptjs";

const userSchema = new mongoose.Schema(
  {
    name: String,

    email: { type: String, required: true, unique: true },

    phone: String,

    password: { type: String, required: true },

    role: {
      type: String,
      enum: ["user", "provider", "admin"],
      default: "user",
    },

    status: {
      type: String,
      enum: ["pending", "approved", "rejected"],
      default: "approved",
    },

    businessName: String,

    subscription: {
      type: String,
      enum: ["free", "premium"],
      default: "free",
    },

    businessType: String,

    /* ===============================
       🔥 VERIFICATION SYSTEM (NEW)
    =============================== */

    verificationStatus: {
      type: String,
      enum: ["not_verified", "pending", "verified", "rejected"],
      default: "not_verified",
      index: true, // ✅ keep only here (remove duplicates elsewhere)
    },

    verificationDocumentType: {
      type: String,
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
  },
  { timestamps: true }
);

// ===============================
// 🔐 PASSWORD HASH
// ===============================
userSchema.pre("save", async function (next) {
  if (!this.isModified("password")) return next();

  const salt = await bcrypt.genSalt(10);
  this.password = await bcrypt.hash(this.password, salt);
  next();
});

// ===============================
// 🔐 PASSWORD COMPARE
// ===============================
userSchema.methods.comparePassword = function (password) {
  return bcrypt.compare(password, this.password);
};

export default mongoose.model("User", userSchema);