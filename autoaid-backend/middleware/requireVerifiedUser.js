import User from "../models/User.js";

export const requireVerifiedUser = async (req, res, next) => {
  try {
    if (!req.user?._id) {
      return res.status(401).json({
        message: "Unauthorized",
        code: "UNAUTHORIZED",
      });
    }

    const user = await User.findById(req.user._id);

    if (!user) {
      return res.status(404).json({
        message: "User not found",
        code: "USER_NOT_FOUND",
      });
    }

    if (user.role !== "user") {
      return res.status(403).json({
        message: "User account required",
        code: "USER_ACCOUNT_REQUIRED",
      });
    }

    const verificationStatus = user.verificationStatus || "not_verified";

    if (verificationStatus !== "verified") {
      return res.status(403).json({
        message: "Identity verification required before making requests",
        code: "USER_NOT_VERIFIED",
        verificationStatus,
      });
    }

    next();
  } catch (error) {
    console.error("requireVerifiedUser error:", error);

    return res.status(500).json({
      message: "Failed to validate user eligibility",
      code: "USER_ELIGIBILITY_CHECK_FAILED",
      error: error.message,
    });
  }
};