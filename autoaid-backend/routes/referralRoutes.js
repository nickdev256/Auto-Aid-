import express from "express";
import { getMyReferralSummary } from "../controllers/referralController.js";
import { protect } from "../middleware/authMiddleware.js";

const router = express.Router();

router.get("/me", protect, getMyReferralSummary);

export default router;