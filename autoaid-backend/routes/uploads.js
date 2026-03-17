import express from "express";
import multer from "multer";
import path from "path";
import crypto from "crypto";
import { protect, authorize } from "../middleware/authMiddleware.js";

const router = express.Router();

/* =================================================
   🔒 SECURITY CONFIG
================================================= */

// Allowed file types
const ALLOWED_MIME_TYPES = [
  "image/jpeg",
  "image/png",
  "image/webp",
  "application/pdf",
];

// Max file size: 5MB
const MAX_FILE_SIZE = 5 * 1024 * 1024;

/* =================================================
   STORAGE CONFIG
================================================= */
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, "uploads/");
  },
  filename: function (req, file, cb) {
    // 🔐 random safe filename
    const ext = path.extname(file.originalname).toLowerCase();
    const safeName = crypto.randomBytes(16).toString("hex") + ext;
    cb(null, safeName);
  },
});

/* =================================================
   FILE FILTER (TYPE CHECK)
================================================= */
const fileFilter = (req, file, cb) => {
  if (!ALLOWED_MIME_TYPES.includes(file.mimetype)) {
    return cb(
      new Error("Invalid file type. Only images and PDFs are allowed."),
      false
    );
  }
  cb(null, true);
};

/* =================================================
   MULTER INSTANCE
================================================= */
const upload = multer({
  storage,
  limits: { fileSize: MAX_FILE_SIZE },
  fileFilter,
});

/* =================================================
   🔒 UPLOAD ROUTE (SECURED)
================================================= */
router.post(
  "/",
  protect,
  authorize("provider", "admin"),
  upload.single("file"),
  (req, res) => {
    if (!req.file) {
      return res.status(400).json({ message: "No file uploaded" });
    }

    res.json({
      success: true,
      file: req.file.filename,
    });
  }
);

export default router;