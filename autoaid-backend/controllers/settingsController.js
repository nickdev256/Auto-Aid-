import Settings from "../models/Settings.js";
import User from "../models/User.js";
import Notification from "../models/Notification.js";
import { sendEmail } from "../utils/sendEmailOTP.js";

const DEFAULT_MAINTENANCE_MESSAGE =
  "AutoAid is currently under maintenance. Please try again later.";

const getOrCreateSettings = async () => {
  let settings = await Settings.findOne();

  if (!settings) {
    settings = await Settings.create({
      systemName: "AutoAid",
      supportEmail: "",
      supportPhone: "",
      whatsappNumber: "",
      emergencyHotline: "",
      notificationsEnabled: true,
      maintenanceMode: false,
      maintenanceMessage: DEFAULT_MAINTENANCE_MESSAGE,
      maintenanceTarget: "both",
      allowUserRegistration: true,
      allowProviderRegistration: true,
      autoApproveProviders: false,
    });
  }

  return settings;
};

export const getAdminSettings = async (req, res) => {
  try {
    const settings = await getOrCreateSettings();

    return res.status(200).json(settings);
  } catch (error) {
    console.error("getAdminSettings error:", error);
    return res.status(500).json({
      message: "Failed to load settings",
    });
  }
};

export const updateAdminSettings = async (req, res) => {
  try {
    const settings = await getOrCreateSettings();

    const {
      systemName,
      supportEmail,
      supportPhone,
      whatsappNumber,
      emergencyHotline,
      notificationsEnabled,
      maintenanceMode,
      maintenanceMessage,
      maintenanceTarget,
      allowUserRegistration,
      allowProviderRegistration,
      autoApproveProviders,
    } = req.body;

    if (supportEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(supportEmail)) {
      return res.status(400).json({
        message: "Please provide a valid support email address.",
      });
    }

    if (
      maintenanceTarget &&
      !["web", "android", "both"].includes(maintenanceTarget)
    ) {
      return res.status(400).json({
        message: "Invalid maintenance target.",
      });
    }

    if (typeof systemName !== "undefined") {
      settings.systemName = String(systemName).trim() || "AutoAid";
    }

    if (typeof supportEmail !== "undefined") {
      settings.supportEmail = String(supportEmail).trim();
    }

    if (typeof supportPhone !== "undefined") {
      settings.supportPhone = String(supportPhone).trim();
    }

    if (typeof whatsappNumber !== "undefined") {
      settings.whatsappNumber = String(whatsappNumber).trim();
    }

    if (typeof emergencyHotline !== "undefined") {
      settings.emergencyHotline = String(emergencyHotline).trim();
    }

    if (typeof notificationsEnabled !== "undefined") {
      settings.notificationsEnabled = Boolean(notificationsEnabled);
    }

    if (typeof maintenanceMode !== "undefined") {
      settings.maintenanceMode = Boolean(maintenanceMode);
    }

    if (typeof maintenanceMessage !== "undefined") {
      settings.maintenanceMessage =
        String(maintenanceMessage).trim() || DEFAULT_MAINTENANCE_MESSAGE;
    }

    if (typeof maintenanceTarget !== "undefined") {
      settings.maintenanceTarget = maintenanceTarget;
    }

    if (typeof allowUserRegistration !== "undefined") {
      settings.allowUserRegistration = Boolean(allowUserRegistration);
    }

    if (typeof allowProviderRegistration !== "undefined") {
      settings.allowProviderRegistration = Boolean(allowProviderRegistration);
    }

    if (typeof autoApproveProviders !== "undefined") {
      settings.autoApproveProviders = Boolean(autoApproveProviders);
    }

    await settings.save();

    return res.status(200).json({
      message: "Settings updated successfully",
      ...settings.toObject(),
    });
  } catch (error) {
    console.error("updateAdminSettings error:", error);
    return res.status(500).json({
      message: "Failed to save settings",
    });
  }
};

export const sendMarketingEmail = async (req, res) => {
  try {
    const {
      audience = "all",
      subject,
      heading,
      message,
      sendEmail: shouldSendEmail = true,
      sendNotification: shouldSendNotification = false,
    } = req.body;

    if (!subject || !String(subject).trim()) {
      return res.status(400).json({
        message: "Email subject is required.",
      });
    }

    if (!message || !String(message).trim()) {
      return res.status(400).json({
        message: "Email message is required.",
      });
    }

    let query = {};

    if (audience === "providers") {
      query.role = "provider";
    } else if (audience === "customers") {
      query.role = "user";
    } else if (audience === "verified_providers") {
      query.role = "provider";
      query["providerVerification.status"] = "verified";
    } else if (audience === "all") {
      query = {};
    } else {
      return res.status(400).json({
        message: "Invalid audience selected.",
      });
    }

    const recipients = await User.find(query).select("name email role");

    if (!recipients.length) {
      return res.status(404).json({
        message: "No recipients found for the selected audience.",
      });
    }

    let emailedCount = 0;
    let notificationCount = 0;

    const safeHeading = heading?.trim() || subject.trim();
    const safeSubject = subject.trim();
    const safeMessage = message.trim();

    const html = `
      <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #111827;">
        <div style="max-width: 640px; margin: 0 auto; padding: 24px; border: 1px solid #e5edf7; border-radius: 16px; background: #ffffff;">
          <div style="margin-bottom: 18px;">
            <span style="display:inline-block;padding:6px 12px;border-radius:999px;background:#eff6ff;color:#1d4ed8;font-size:12px;font-weight:700;">
              AutoAid Admin Communication
            </span>
          </div>
          <h2 style="margin:0 0 12px;color:#0f172a;">${safeHeading}</h2>
          <p style="margin:0;color:#475569;white-space:pre-line;">${safeMessage}</p>
        </div>
      </div>
    `;

    if (shouldSendEmail) {
      for (const recipient of recipients) {
        if (!recipient.email) continue;

        try {
          await sendEmail({
            to: recipient.email,
            subject: safeSubject,
            text: safeMessage,
            html,
          });
          emailedCount += 1;
        } catch (emailError) {
          console.error(
            `Failed to send email to ${recipient.email}:`,
            emailError.message
          );
        }
      }
    }

    if (shouldSendNotification) {
      const notificationDocs = recipients.map((recipient) => ({
        user: recipient._id,
        title: safeHeading,
        message: safeMessage,
        type: "admin_message",
        read: false,
      }));

      if (notificationDocs.length) {
        await Notification.insertMany(notificationDocs);
        notificationCount = notificationDocs.length;
      }
    }

    return res.status(200).json({
      message: "Communication sent successfully.",
      audience,
      recipients: recipients.length,
      emailedCount,
      notificationCount,
    });
  } catch (error) {
    console.error("sendMarketingEmail error:", error);
    return res.status(500).json({
      message: "Failed to send communication.",
    });
  }
};