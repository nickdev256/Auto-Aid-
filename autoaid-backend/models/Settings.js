import mongoose from "mongoose";

const SettingsSchema = new mongoose.Schema(
  {
    systemName: {
      type: String,
      default: "AutoAid",
    },
    supportEmail: {
      type: String,
      default: "",
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
      default: "AutoAid is currently under maintenance. Please try again later.",
    },
  },
  { timestamps: true }
);

export default mongoose.model("Settings", SettingsSchema);