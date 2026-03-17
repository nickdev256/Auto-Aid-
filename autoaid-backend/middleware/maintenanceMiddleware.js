import Settings from "../models/Settings.js";

const ADMIN_ALLOWED_PREFIXES = [
  "/api/admin",
  "/api/ping",
  "/",
];

export async function checkMaintenance(req, res, next) {
  try {
    const path = req.path || req.originalUrl || "";

    const isAllowedDuringMaintenance = ADMIN_ALLOWED_PREFIXES.some((prefix) =>
      path === prefix || path.startsWith(prefix + "/")
    );

    // allow uploads static access if you want admin panels/images to keep working
    if (path.startsWith("/uploads")) {
      return next();
    }

    if (isAllowedDuringMaintenance) {
      return next();
    }

    const settings = await Settings.findOne().lean();

    const maintenanceMode = !!settings?.maintenanceMode;
    const maintenanceMessage =
      settings?.maintenanceMessage ||
      "AutoAid is currently under maintenance. Please try again later.";

    if (!maintenanceMode) {
      return next();
    }

    return res.status(503).json({
      ok: false,
      maintenanceMode: true,
      message: maintenanceMessage,
      systemName: settings?.systemName || "AutoAid",
    });
  } catch (err) {
    console.error("❌ Maintenance middleware error:", err);
    return next();
  }
}