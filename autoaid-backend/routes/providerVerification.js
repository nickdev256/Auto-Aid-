import express from "express";
import multer from "multer";
import path from "path";
import fs from "fs";
import jwt from "jsonwebtoken";
import User from "../models/User.js";

const router = express.Router();

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  throw new Error("JWT_SECRET missing in environment");
}

const uploadDir = "uploads/provider-verification";
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

function guessExtensionFromMime(mimetype = "") {
  const mime = String(mimetype).toLowerCase();

  if (mime === "image/jpeg" || mime === "image/jpg") return ".jpg";
  if (mime === "image/png") return ".png";
  if (mime === "image/webp") return ".webp";
  if (mime === "application/pdf") return ".pdf";

  return "";
}

function normalizePath(filePath = "") {
  return String(filePath).replace(/\\/g, "/");
}

function toPublicUrl(req, filePath = "") {
  const normalized = normalizePath(filePath);
  if (!normalized) return "";

  if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
    return normalized;
  }

  const cleanPath = normalized.startsWith("/") ? normalized : `/${normalized}`;
  return `${req.protocol}://${req.get("host")}${cleanPath}`;
}

function getToken(req) {
  const auth = req.headers.authorization || "";
  if (auth.startsWith("Bearer ")) return auth.slice(7);
  return req.cookies?.token || null;
}

async function getAuthenticatedUser(req) {
  const token = getToken(req);
  if (!token) return null;

  const decoded = jwt.verify(token, JWT_SECRET);
  if (!decoded?.id) return null;

  return await User.findById(decoded.id);
}

function isAdmin(user) {
  return String(user?.role || "").toLowerCase() === "admin";
}

function buildVerificationUpdateBase() {
  return {
    verificationStatus: "pending",
    verificationSubmittedAt: new Date(),
    verificationReviewedAt: null,
    verificationRejectionReason: "",
  };
}

function buildUser(req, userDoc) {
  const user =
    typeof userDoc?.getDecrypted === "function"
      ? userDoc.getDecrypted()
      : userDoc?.toObject?.() || userDoc;

  return {
    id: user?._id || "",
    _id: user?._id || "",
    name: user?.name || "",
    fullName: user?.name || "",
    email: user?.email || "",
    phone: user?.phone || "",
    role: user?.role || "provider",
    status: user?.status || "pending",

    businessName: user?.businessName || "",
    businessType: user?.businessType || "",
    servicesOffered: Array.isArray(user?.servicesOffered)
      ? user.servicesOffered
      : [],

    verificationStatus: user?.verificationStatus || "not_verified",
    verificationSubmittedAt: user?.verificationSubmittedAt || null,
    verificationReviewedAt: user?.verificationReviewedAt || null,
    verificationRejectionReason: user?.verificationRejectionReason || "",

    workLicenseDocumentUrl: toPublicUrl(req, user?.workLicenseDocumentUrl || ""),
    businessRegistrationDocumentUrl: toPublicUrl(
      req,
      user?.businessRegistrationDocumentUrl || ""
    ),
    nationalIdFrontUrl: toPublicUrl(req, user?.nationalIdFrontUrl || ""),
    nationalIdBackUrl: toPublicUrl(req, user?.nationalIdBackUrl || ""),
    profileImageUrl: toPublicUrl(req, user?.profileImage || ""),

    isApprovedProvider: user?.isApprovedProvider === true,
    isAvailable: user?.isAvailable === true,
    isOnline: user?.isOnline === true,

    subscription: user?.subscription || {
      plan: "free",
      active: false,
      startDate: null,
      expiryDate: null,
      paymentMethod: null,
      price: 0,
    },

    providerVerification: {
      status: user?.verificationStatus || "not_verified",
      rejectionReason: user?.verificationRejectionReason || "",
      licenseDocumentUrl: toPublicUrl(req, user?.workLicenseDocumentUrl || ""),
      businessDocumentUrl: toPublicUrl(
        req,
        user?.businessRegistrationDocumentUrl || ""
      ),
      nationalIdFrontUrl: toPublicUrl(req, user?.nationalIdFrontUrl || ""),
      nationalIdBackUrl: toPublicUrl(req, user?.nationalIdBackUrl || ""),
      profileImageUrl: toPublicUrl(req, user?.profileImage || ""),
      submittedAt: user?.verificationSubmittedAt || null,
      reviewedAt: user?.verificationReviewedAt || null,
    },
  };
}

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, uploadDir),
  filename: (_req, file, cb) => {
    const originalName = String(file.originalname || "file");
    const extFromName = path.extname(originalName);
    const extFromMime = guessExtensionFromMime(file.mimetype);
    const ext = extFromName || extFromMime || ".jpg";

    const safeBase =
      path
        .basename(originalName, extFromName || path.extname(originalName))
        .replace(/[^a-zA-Z0-9_-]/g, "_") || "file";

    cb(null, `${safeBase}-${Date.now()}${ext}`);
  },
});

const fileFilter = (_req, file, cb) => {
  const allowedMimeTypes = [
    "image/jpeg",
    "image/jpg",
    "image/png",
    "image/webp",
    "application/pdf",
    "application/octet-stream",
  ];

  const allowedExtensions = [".jpg", ".jpeg", ".png", ".webp", ".pdf"];

  const originalName = String(file.originalname || "").toLowerCase();
  const mimetype = String(file.mimetype || "").toLowerCase();

  const hasAllowedMime = allowedMimeTypes.includes(mimetype);
  const hasAllowedExtension = allowedExtensions.some((ext) =>
    originalName.endsWith(ext)
  );

  if (hasAllowedMime || hasAllowedExtension) {
    return cb(null, true);
  }

  return cb(
    new Error(
      `Unsupported file type. Allowed: JPG, JPEG, PNG, WEBP, PDF. Received mime: ${
        mimetype || "unknown"
      }`
    )
  );
};

const upload = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: 10 * 1024 * 1024,
  },
});

router.get("/me", async (req, res) => {
  try {
    const authUser = await getAuthenticatedUser(req);

    if (!authUser) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (String(authUser.role || "").toLowerCase() !== "provider") {
      return res.status(403).json({
        message: "Only providers can access verification details",
      });
    }

    const user = buildUser(req, authUser);

    return res.json({
      message: "Provider verification fetched successfully",
      user,
      provider: {
        fullName: user.fullName,
        phone: user.phone,
        providerVerification: user.providerVerification,
      },
      verificationStatus: authUser.verificationStatus || "not_verified",
    });
  } catch (e) {
    console.error("provider verification me error:", e);
    return res.status(500).json({
      message: e.message || "Failed to fetch provider verification",
    });
  }
});

router.post(
  "/submit",
  upload.fields([
    { name: "workLicenseDocument", maxCount: 1 },
    { name: "businessRegistrationDocument", maxCount: 1 },
    { name: "nationalIdFront", maxCount: 1 },
    { name: "nationalIdBack", maxCount: 1 },
    { name: "profileImage", maxCount: 1 },
  ]),
  async (req, res) => {
    try {
      const authUser = await getAuthenticatedUser(req);

      if (!authUser) {
        return res.status(401).json({ message: "Unauthorized" });
      }

      if (String(authUser.role || "").toLowerCase() !== "provider") {
        return res.status(403).json({
          message: "Only providers can submit provider verification",
        });
      }

      const businessName = String(req.body?.businessName || "").trim();
      const phone = String(req.body?.phone || "").trim();
      const businessType = String(req.body?.businessType || "")
        .trim()
        .toLowerCase();

      const workLicenseFile = req.files?.workLicenseDocument?.[0] || null;
      const businessRegFile =
        req.files?.businessRegistrationDocument?.[0] || null;
      const nationalIdFrontFile = req.files?.nationalIdFront?.[0] || null;
      const nationalIdBackFile = req.files?.nationalIdBack?.[0] || null;
      const profileImageFile = req.files?.profileImage?.[0] || null;

      if (
        !workLicenseFile &&
        !businessRegFile &&
        !nationalIdFrontFile &&
        !nationalIdBackFile &&
        !profileImageFile &&
        !businessName &&
        !phone &&
        !businessType
      ) {
        return res.status(400).json({
          message: "At least one verification field is required",
        });
      }

      const update = {
        ...buildVerificationUpdateBase(),
        status: "pending",
        isApprovedProvider: false,
        isAvailable: false,
      };

      if (businessName) update.businessName = businessName;
      if (phone) update.phone = phone;

      if (
        businessType &&
        ["garage", "towing", "fuel", "ambulance"].includes(businessType)
      ) {
        update.businessType = businessType;

        if (
          !Array.isArray(authUser.servicesOffered) ||
          authUser.servicesOffered.length === 0
        ) {
          update.servicesOffered = [businessType];
        }
      }

      if (workLicenseFile) {
        update.workLicenseDocumentUrl = normalizePath(workLicenseFile.path);
      }

      if (businessRegFile) {
        update.businessRegistrationDocumentUrl = normalizePath(
          businessRegFile.path
        );
      }

      if (nationalIdFrontFile) {
        update.nationalIdFrontUrl = normalizePath(nationalIdFrontFile.path);
      }

      if (nationalIdBackFile) {
        update.nationalIdBackUrl = normalizePath(nationalIdBackFile.path);
      }

      if (profileImageFile) {
        update.profileImage = normalizePath(profileImageFile.path);
      }

      const updatedUser = await User.findByIdAndUpdate(authUser._id, update, {
        new: true,
        runValidators: true,
      });

      if (!updatedUser) {
        return res.status(404).json({ message: "Provider not found" });
      }

      return res.json({
        message: "Provider verification submitted successfully",
        user: buildUser(req, updatedUser),
        verificationStatus: updatedUser.verificationStatus || "pending",
      });
    } catch (e) {
      console.error("provider verification submit error:", e);
      return res.status(500).json({
        message: e.message || "Provider verification submission failed",
      });
    }
  }
);

router.get("/admin/pending", async (req, res) => {
  try {
    const authUser = await getAuthenticatedUser(req);

    if (!authUser) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!isAdmin(authUser)) {
      return res.status(403).json({ message: "Admin only" });
    }

    const providers = await User.find({
      role: "provider",
      verificationStatus: "pending",
    }).sort({ verificationSubmittedAt: -1 });

    return res.json({
      message: "Pending provider verifications fetched successfully",
      providers: providers.map((p) => buildUser(req, p)),
    });
  } catch (e) {
    console.error("pending provider verifications error:", e);
    return res.status(500).json({
      message: e.message || "Failed to fetch pending verifications",
    });
  }
});

router.patch("/admin/:id/approve", async (req, res) => {
  try {
    const authUser = await getAuthenticatedUser(req);

    if (!authUser) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!isAdmin(authUser)) {
      return res.status(403).json({ message: "Admin only" });
    }

    const targetUser = await User.findOne({
      _id: req.params.id,
      role: "provider",
    });

    if (!targetUser) {
      return res.status(404).json({ message: "Provider not found" });
    }

    const updatedUser = await User.findByIdAndUpdate(
      req.params.id,
      {
        verificationStatus: "verified",
        verificationReviewedAt: new Date(),
        verificationRejectionReason: "",
        status: "approved",
        isApprovedProvider: true,
      },
      { new: true, runValidators: true }
    );

    return res.json({
      message: "Provider verification approved",
      user: buildUser(req, updatedUser),
      verificationStatus: updatedUser?.verificationStatus || "verified",
    });
  } catch (e) {
    console.error("provider verification approve error:", e);
    return res.status(500).json({
      message: e.message || "Verification approval failed",
    });
  }
});

router.patch("/admin/:id/reject", async (req, res) => {
  try {
    const authUser = await getAuthenticatedUser(req);

    if (!authUser) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!isAdmin(authUser)) {
      return res.status(403).json({ message: "Admin only" });
    }

    const reason = String(req.body?.reason || "").trim();

    const targetUser = await User.findOne({
      _id: req.params.id,
      role: "provider",
    });

    if (!targetUser) {
      return res.status(404).json({ message: "Provider not found" });
    }

    const updatedUser = await User.findByIdAndUpdate(
      req.params.id,
      {
        verificationStatus: "rejected",
        verificationReviewedAt: new Date(),
        verificationRejectionReason: reason || "Verification rejected",
        isApprovedProvider: false,
        isAvailable: false,
      },
      { new: true, runValidators: true }
    );

    return res.json({
      message: "Provider verification rejected",
      user: buildUser(req, updatedUser),
      verificationStatus: updatedUser?.verificationStatus || "rejected",
    });
  } catch (e) {
    console.error("provider verification reject error:", e);
    return res.status(500).json({
      message: e.message || "Verification rejection failed",
    });
  }
});

router.use((err, _req, res, next) => {
  if (!err) return next();

  if (err instanceof multer.MulterError) {
    return res.status(400).json({
      message: err.message || "Upload error",
    });
  }

  return res.status(400).json({
    message: err.message || "Invalid upload request",
  });
});

export default router;