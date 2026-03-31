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

/* =================================================
   UPLOAD DIRECTORY
================================================= */
const uploadDir = "uploads/user-verification";
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

/* =================================================
   HELPERS
================================================= */
function normalizePath(filePath = "") {
  return String(filePath).replace(/\\/g, "/");
}

function toPublicUrl(req, filePath = "") {
  const normalized = normalizePath(filePath);
  if (!normalized) return "";

  if (
    normalized.startsWith("http://") ||
    normalized.startsWith("https://")
  ) {
    return normalized;
  }

  const cleanPath = normalized.startsWith("/")
    ? normalized
    : `/${normalized}`;

  return `${req.protocol}://${req.get("host")}${cleanPath}`;
}

function guessExtensionFromMime(mimetype = "") {
  const mime = String(mimetype).toLowerCase();

  if (mime === "image/jpeg" || mime === "image/jpg") return ".jpg";
  if (mime === "image/png") return ".png";
  if (mime === "image/webp") return ".webp";
  if (mime === "application/pdf") return ".pdf";

  return "";
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

function normalizeDocumentType(value = "") {
  const raw = String(value || "").trim();

  const docTypeMap = {
    "National ID": "national_id",
    "national id": "national_id",
    national_id: "national_id",

    Passport: "passport",
    passport: "passport",

    "Driver's License": "drivers_license",
    "Drivers License": "drivers_license",
    "drivers license": "drivers_license",
    drivers_license: "drivers_license",
  };

  return docTypeMap[raw] || raw.toLowerCase().trim();
}

function isValidDocumentType(value = "") {
  const allowedDocumentTypes = [
    "national_id",
    "passport",
    "drivers_license",
  ];
  return allowedDocumentTypes.includes(value);
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
    email: user?.email || "",
    phone: user?.phone || "",
    role: user?.role || "user",
    status: user?.status || "active",

    verificationStatus: user?.verificationStatus || "not_verified",
    verificationDocumentType: user?.verificationDocumentType || "",
    verificationSubmittedAt: user?.verificationSubmittedAt || null,
    verificationReviewedAt: user?.verificationReviewedAt || null,
    verificationRejectionReason: user?.verificationRejectionReason || "",

    verificationDocumentUrl: toPublicUrl(
      req,
      user?.verificationDocumentUrl || ""
    ),
    profileImageUrl: toPublicUrl(req, user?.profileImage || ""),

    verificationDocumentPath: user?.verificationDocumentUrl || "",
    profileImagePath: user?.profileImage || "",
  };
}

function cleanupUploadedFiles(req) {
  try {
    const uploadedFiles = [
      ...(req.files?.verificationDocument || []),
      ...(req.files?.profileImage || []),
    ];

    uploadedFiles.forEach((file) => {
      if (file?.path && fs.existsSync(file.path)) {
        fs.unlinkSync(file.path);
      }
    });
  } catch (cleanupError) {
    console.error("file cleanup error:", cleanupError);
  }
}

/* =================================================
   MULTER CONFIG
================================================= */
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

/* =================================================
   GET CURRENT USER VERIFICATION
================================================= */
router.get("/me", async (req, res) => {
  try {
    const authUser = await getAuthenticatedUser(req);

    if (!authUser) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (String(authUser.role || "").toLowerCase() !== "user") {
      return res.status(403).json({
        message: "Only users can access identity verification details",
      });
    }

    const user = buildUser(req, authUser);

    return res.json({
      message: "User verification fetched successfully",
      user,
      verificationStatus: authUser.verificationStatus || "not_verified",
    });
  } catch (e) {
    console.error("user verification me error:", e);
    return res.status(500).json({
      message: e.message || "Failed to fetch user verification",
    });
  }
});

/* =================================================
   POST /submit
================================================= */
router.post(
  "/submit",
  upload.fields([
    { name: "verificationDocument", maxCount: 1 },
    { name: "profileImage", maxCount: 1 },
  ]),
  async (req, res) => {
    try {
      const authUser = await getAuthenticatedUser(req);

      if (!authUser) {
        cleanupUploadedFiles(req);
        return res.status(401).json({ message: "Unauthorized" });
      }

      if (String(authUser.role || "").toLowerCase() !== "user") {
        cleanupUploadedFiles(req);
        return res.status(403).json({
          message: "Only users can submit identity verification",
        });
      }

      const rawDocumentType = String(req.body?.documentType || "").trim();
      const normalizedDocumentType = normalizeDocumentType(rawDocumentType);

      const verificationDocumentFile =
        req.files?.verificationDocument?.[0] || null;
      const profileImageFile = req.files?.profileImage?.[0] || null;

      console.log("=== USER VERIFICATION SUBMIT DEBUG ===");
      console.log("req.body:", req.body);
      console.log("documentType raw:", rawDocumentType);
      console.log("documentType normalized:", normalizedDocumentType);
      console.log(
        "verificationDocument present:",
        Boolean(verificationDocumentFile)
      );
      console.log("profileImage present:", Boolean(profileImageFile));
      console.log("req.files keys:", Object.keys(req.files || {}));

      if (!rawDocumentType) {
        cleanupUploadedFiles(req);
        return res.status(400).json({
          message:
            "documentType is required. Allowed values: national_id, passport, drivers_license",
        });
      }

      if (!isValidDocumentType(normalizedDocumentType)) {
        cleanupUploadedFiles(req);
        return res.status(400).json({
          message: `Unsupported documentType: ${rawDocumentType}. Allowed values: national_id, passport, drivers_license`,
        });
      }

      if (!verificationDocumentFile) {
        cleanupUploadedFiles(req);
        return res.status(400).json({
          message:
            "verificationDocument file is required. Send it as multipart/form-data with field name 'verificationDocument'",
        });
      }

      const update = {
        verificationStatus: "pending",
        verificationDocumentType: normalizedDocumentType,
        verificationSubmittedAt: new Date(),
        verificationReviewedAt: null,
        verificationRejectionReason: "",
        verificationDocumentUrl: normalizePath(verificationDocumentFile.path),
      };

      if (profileImageFile) {
        update.profileImage = normalizePath(profileImageFile.path);
      }

      const updatedUser = await User.findByIdAndUpdate(authUser._id, update, {
        new: true,
        runValidators: true,
      });

      if (!updatedUser) {
        cleanupUploadedFiles(req);
        return res.status(404).json({ message: "User not found" });
      }

      return res.json({
        message: "User verification submitted successfully",
        user: buildUser(req, updatedUser),
        verificationStatus: updatedUser.verificationStatus || "pending",
      });
    } catch (e) {
      console.error("user verification submit error:", e);
      cleanupUploadedFiles(req);
      return res.status(500).json({
        message: e.message || "User verification submission failed",
      });
    }
  }
);

/* =================================================
   ADMIN: LIST PENDING USERS
================================================= */
router.get("/admin/pending", async (req, res) => {
  try {
    const authUser = await getAuthenticatedUser(req);

    if (!authUser) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!isAdmin(authUser)) {
      return res.status(403).json({ message: "Admin only" });
    }

    const users = await User.find({
      role: "user",
      verificationStatus: "pending",
    }).sort({ verificationSubmittedAt: -1 });

    return res.json({
      message: "Pending user verifications fetched successfully",
      users: users.map((u) => buildUser(req, u)),
    });
  } catch (e) {
    console.error("pending user verifications error:", e);
    return res.status(500).json({
      message: e.message || "Failed to fetch pending user verifications",
    });
  }
});

/* =================================================
   ADMIN: LIST ALL USERS
================================================= */
router.get("/admin/all", async (req, res) => {
  try {
    const authUser = await getAuthenticatedUser(req);

    if (!authUser) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!isAdmin(authUser)) {
      return res.status(403).json({ message: "Admin only" });
    }

    const users = await User.find({
      role: "user",
    }).sort({ verificationSubmittedAt: -1, createdAt: -1 });

    return res.json({
      message: "User verifications fetched successfully",
      users: users.map((u) => buildUser(req, u)),
    });
  } catch (e) {
    console.error("all user verifications error:", e);
    return res.status(500).json({
      message: e.message || "Failed to fetch user verifications",
    });
  }
});

/* =================================================
   ADMIN: GET ONE USER
================================================= */
router.get("/admin/:id", async (req, res) => {
  try {
    const authUser = await getAuthenticatedUser(req);

    if (!authUser) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!isAdmin(authUser)) {
      return res.status(403).json({ message: "Admin only" });
    }

    const user = await User.findOne({
      _id: req.params.id,
      role: "user",
    });

    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    return res.json({
      message: "User verification fetched successfully",
      user: buildUser(req, user),
    });
  } catch (e) {
    console.error("single user verification error:", e);
    return res.status(500).json({
      message: e.message || "Failed to fetch user verification",
    });
  }
});

/* =================================================
   ADMIN APPROVE
================================================= */
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
      role: "user",
    });

    if (!targetUser) {
      return res.status(404).json({ message: "User not found" });
    }

    const updatedUser = await User.findByIdAndUpdate(
      req.params.id,
      {
        verificationStatus: "verified",
        verificationReviewedAt: new Date(),
        verificationRejectionReason: "",
      },
      { new: true, runValidators: true }
    );

    return res.json({
      message: "User verification approved",
      user: buildUser(req, updatedUser),
      verificationStatus: updatedUser?.verificationStatus || "verified",
    });
  } catch (e) {
    console.error("user verification approve error:", e);
    return res.status(500).json({
      message: e.message || "Verification approval failed",
    });
  }
});

/* =================================================
   ADMIN REJECT
================================================= */
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
      role: "user",
    });

    if (!targetUser) {
      return res.status(404).json({ message: "User not found" });
    }

    const updatedUser = await User.findByIdAndUpdate(
      req.params.id,
      {
        verificationStatus: "rejected",
        verificationReviewedAt: new Date(),
        verificationRejectionReason: reason || "Verification rejected",
      },
      { new: true, runValidators: true }
    );

    return res.json({
      message: "User verification rejected",
      user: buildUser(req, updatedUser),
      verificationStatus: updatedUser?.verificationStatus || "rejected",
    });
  } catch (e) {
    console.error("user verification reject error:", e);
    return res.status(500).json({
      message: e.message || "Verification rejection failed",
    });
  }
});

/* =================================================
   MULTER ERROR HANDLER
================================================= */
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