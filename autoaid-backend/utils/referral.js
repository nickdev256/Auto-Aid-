import User from "../models/User.js";
import Referral from "../models/Referral.js";

export const REFERRAL_DISCOUNT_AMOUNT = 5000;

export function generateReferralCodeFromName(name = "AUTOAID") {
  const cleaned = String(name || "AUTOAID")
    .replace(/[^a-zA-Z0-9]/g, "")
    .toUpperCase();

  const prefix = (cleaned || "AUTOAID").slice(0, 6);
  const suffix = Math.floor(1000 + Math.random() * 9000);

  return `${prefix}${suffix}`;
}

export async function generateUniqueReferralCode(name) {
  for (let i = 0; i < 20; i++) {
    const code = generateReferralCodeFromName(name);
    const exists = await User.findOne({ referralCode: code }).lean();
    if (!exists) return code;
  }

  return `AUTO${Date.now().toString().slice(-6)}`;
}

export function applyDiscount(baseAmount, discountAmount) {
  const originalAmount = Number(baseAmount || 0);
  const safeDiscount = Math.max(0, Number(discountAmount || 0));
  const appliedDiscount = Math.min(originalAmount, safeDiscount);
  const finalAmount = Math.max(0, originalAmount - appliedDiscount);

  return {
    originalAmount,
    discountAmount: appliedDiscount,
    finalAmount,
  };
}

export async function getUserActiveReferralDiscount(userId) {
  const user = await User.findById(userId).lean();
  if (!user) {
    return { discountAmount: 0, reason: null };
  }

  if ((user.nextReferralDiscountAmount || 0) > 0) {
    return {
      discountAmount: user.nextReferralDiscountAmount,
      reason: "referrer_reward",
    };
  }

  if (user.referredBy && !user.hasUsedReferralDiscount) {
    const referral = await Referral.findOne({
      referredUserId: user._id,
    }).lean();

    if (referral && ["signed_up", "discount_applied"].includes(referral.status)) {
      return {
        discountAmount:
          referral.friendDiscountAmount || REFERRAL_DISCOUNT_AMOUNT,
        reason: "referred_friend_first_job",
      };
    }
  }

  return { discountAmount: 0, reason: null };
}