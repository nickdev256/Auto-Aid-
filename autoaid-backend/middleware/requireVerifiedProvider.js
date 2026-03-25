import User from "../models/User.js";

export const requireVerifiedProvider = async (req, res, next) => {
  try {
    if (!req.user?._id) {
      return res.status(401).json({
        message: "Unauthorized",
        code: "UNAUTHORIZED",
      });
    }

    const provider = await User.findById(req.user._id);

    if (!provider) {
      return res.status(404).json({
        message: "Provider not found",
        code: "PROVIDER_NOT_FOUND",
      });
    }

    if (provider.role !== "provider") {
      return res.status(403).json({
        message: "Provider account required",
        code: "PROVIDER_ACCOUNT_REQUIRED",
      });
    }

    const verificationStatus = provider.verificationStatus || "not_verified";
    const subscriptionActive = provider.subscription?.active === true;

    if (verificationStatus !== "verified") {
      return res.status(403).json({
        message: "Verification required before receiving jobs",
        code: "PROVIDER_NOT_VERIFIED",
        verificationStatus,
      });
    }

    if (!subscriptionActive) {
      return res.status(403).json({
        message: "Subscription inactive",
        code: "SUBSCRIPTION_INACTIVE",
      });
    }

    if (!provider.isApprovedProvider || provider.status !== "approved") {
      return res.status(403).json({
        message: "Provider account is not approved yet",
        code: "PROVIDER_NOT_APPROVED",
        status: provider.status,
        isApprovedProvider: provider.isApprovedProvider,
      });
    }

    next();
  } catch (error) {
    console.error("requireVerifiedProvider error:", error);

    return res.status(500).json({
      message: "Failed to validate provider eligibility",
      code: "PROVIDER_ELIGIBILITY_CHECK_FAILED",
      error: error.message,
    });
  }
};