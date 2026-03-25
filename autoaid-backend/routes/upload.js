import express from "express";

const router = express.Router();

// TEST ROUTE
router.get("/", (req, res) => {
  res.json({ message: "Uploads route working ✅" });
});

export default router;