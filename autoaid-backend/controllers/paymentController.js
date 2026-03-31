import Payment from "../models/Payment.js";

export const getPaymentHistory = async (req, res) => {
  try {
    const userId = req.user._id;

    const payments = await Payment.find({ userId })
      .sort({ createdAt: -1 })
      .lean();

    const formatted = payments.map((p) => ({
      _id: p._id,
      requestId: p.requestId,
      providerName: p.providerName || "Provider",
      serviceName: p.serviceName || "Service",
      amount: p.amount,
      method: p.method,
      paymentStatus: p.paymentStatus,
      paymentConfirmedByProvider: p.paymentConfirmedByProvider,
      reference: p.reference,
      createdAt: p.createdAt,
    }));

    res.json(formatted);
  } catch (error) {
    console.error("❌ Payment history error:", error);
    res.status(500).json({
      message: "Failed to fetch payment history",
    });
  }
};