import Referral from "../models/Referral.js";
import User from "../models/User.js";

export async function getMyReferralSummary(req, res) {
  try {
    const userId = req.user?.id || req.user?._id;

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    const referrals = await Referral.find({ referrerUserId: userId })
      .populate("referredUserId", "name email phone")
      .sort({ createdAt: -1 });

    const totalReferrals = referrals.length;
    const rewardedCount = referrals.filter((r) => r.status === "rewarded").length;

    return res.json({
      ok: true,
      referralCode: user.referralCode || "",
      nextReferralDiscountAmount: Number(user.nextReferralDiscountAmount || 0),
      totalReferrals,
      rewardedCount,
      referrals: referrals.map((r) => ({
        _id: r._id,
        referredUser: r.referredUserId
          ? {
              _id: r.referredUserId._id,
              name: r.referredUserId.name || "",
              email: r.referredUserId.email || "",
              phone:
                typeof r.referredUserId.getDecrypted === "function"
                  ? r.referredUserId.getDecrypted().phone || ""
                  : r.referredUserId.phone || "",
            }
          : null,
        referralCode: r.referralCode,
        status: r.status,
        friendDiscountAmount: Number(r.friendDiscountAmount || 0),
        referrerRewardAmount: Number(r.referrerRewardAmount || 0),
        qualifyingRequestId: r.qualifyingRequestId || null,
        createdAt: r.createdAt,
        updatedAt: r.updatedAt,
        rewardedAt: r.rewardedAt,
      })),
    });
  } catch (error) {
    console.error("getMyReferralSummary error:", error);
    return res.status(500).json({
      message: error.message || "Failed to load referral summary",
    });
  }
}