import express from "express";
import {
  getAdminSettings,
  updateAdminSettings,
  sendMarketingEmail,
} from "../controllers/settingsController.js";
import { protect, authorize } from "../middleware/authMiddleware.js";

const router = express.Router();

router.get("/settings", protect, authorize("admin"), getAdminSettings);
router.put("/settings", protect, authorize("admin"), updateAdminSettings);
router.post(
  "/marketing-email/send",
  protect,
  authorize("admin"),
  sendMarketingEmail
);

export default router;