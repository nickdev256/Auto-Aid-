import express from "express";
import {
  analyzeProblem,
  escalateAfterFailedSelfSolve,
} from "../controllers/aiAssistController.js";

const router = express.Router();

router.post("/analyze", analyzeProblem);
router.post("/escalate", escalateAfterFailedSelfSolve);

export default router;