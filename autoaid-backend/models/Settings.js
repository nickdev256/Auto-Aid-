import mongoose from "mongoose";

const SettingsSchema = new mongoose.Schema(
  {
    systemName: {
      type: String,
      default: "AutoAid",
      trim: true,
    },

    supportEmail: {
      type: String,
      default: "",
      trim: true,
    },

    supportPhone: {
      type: String,
      default: "",
      trim: true,
    },

    whatsappNumber: {
      type: String,
      default: "",
      trim: true,
    },

    emergencyHotline: {
      type: String,
      default: "",
      trim: true,
    },

    notificationsEnabled: {
      type: Boolean,
      default: true,
    },

    maintenanceMode: {
      type: Boolean,
      default: false,
    },

    maintenanceMessage: {
      type: String,
      default:
        "AutoAid is currently under maintenance. Please try again later.",
      trim: true,
    },

    maintenanceTarget: {
      type: String,
      enum: ["web", "android", "both"],
      default: "both",
    },

    allowUserRegistration: {
      type: Boolean,
      default: true,
    },

    allowProviderRegistration: {
      type: Boolean,
      default: true,
    },

    autoApproveProviders: {
      type: Boolean,
      default: false,
    },
  },
  { timestamps: true }
);

export default mongoose.model("Settings", SettingsSchema);