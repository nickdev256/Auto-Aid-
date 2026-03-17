import React, { useEffect, useState } from "react";
import { getProviderSubscriptionStatus, startSubscriptionPayment } from "../../services/api";
import { useNavigate } from "react-router-dom";
import "../../Provider/ProviderSubscription.css";

const PLANS = [
  { id: "monthly", name: "Monthly", price: 50000, duration: "30 days", recommended: false },
  { id: "quarterly", name: "Quarterly", price: 100000, duration: "90 days", recommended: true },
  { id: "yearly", name: "Yearly", price: 250000, duration: "365 days", recommended: false },
];

export default function AdminSubscriptions() {
  const navigate = useNavigate();
  const providerId = localStorage.getItem("pendingProviderId");

  const [subStatus, setSubStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [phone, setPhone] = useState("");
  const [network, setNetwork] = useState("mtn");
  const [processing, setProcessing] = useState(false);
  const [popup, setPopup] = useState(null);

  // redirect if no provider
  useEffect(() => {
    if (!providerId) navigate("/signup", { replace: true });
  }, [providerId, navigate]);

  // load subscription
  const loadStatus = async () => {
    if (!providerId) return null;
    try {
      const data = await getProviderSubscriptionStatus(providerId);
      setSubStatus(data);
      return data;
    } catch (err) {
      console.error(err);
      return null;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStatus();
  }, [providerId]);

  const clearTemp = () => {
    localStorage.removeItem("pendingProviderId");
    localStorage.removeItem("pendingProviderEmail");
    localStorage.removeItem("pendingEmail");
  };

  const openPlanModal = (plan) => {
    setSelectedPlan(plan);
    setShowModal(true);
  };

  const closeModal = () => {
    if (processing) return;
    setShowModal(false);
    setPhone("");
    setNetwork("mtn");
  };

  const handlePay = async (e) => {
    e.preventDefault();

    if (!phone) return setPopup({ type: "info", message: "Enter phone number" });

    setProcessing(true);

    try {
      const res = await startSubscriptionPayment({
        providerId,
        planId: selectedPlan.id,
        phone,
        network,
      });

      if (res?.success && (res?.sandboxMode || res?.subscription?.active)) {
        setPopup({ type: "success", message: "Subscription activated. Please login." });
        clearTemp();
        closeModal();
        setTimeout(() => navigate("/login", { replace: true }), 1000);
        return;
      }

      setPopup({ type: "info", message: "Payment started. Approve on phone..." });
      closeModal();

      for (let i = 0; i < 3; i++) {
        await new Promise((r) => setTimeout(r, 6000));
        const fresh = await loadStatus();
        if (fresh?.subscription?.active) {
          setPopup({ type: "success", message: "Subscription activated. Please login." });
          clearTemp();
          setTimeout(() => navigate("/login", { replace: true }), 900);
          return;
        }
      }

      setPopup({ type: "info", message: "Still pending. Try login shortly." });

    } catch (err) {
      setPopup({ type: "info", message: err.message || "Payment failed" });
    } finally {
      setProcessing(false);
    }
  };

  if (loading) return <p>Loading...</p>;

  const isActive = subStatus?.subscription?.active === true;

  return (
    <div className="subscription-page">
      <button className="back-btn" onClick={() => navigate("/otp", { replace: true })}>
        ← Back
      </button>

      <h1 className="page-title">Subscription Plans</h1>

      <div className="current-sub-box">
        <h3>Current Subscription</h3>

        {isActive ? (
          <p className="active-sub">
            You are on <strong>{subStatus.subscription.plan}</strong>
            <span className="active-pill"> ACTIVE</span>
            <br />
            Expires: {new Date(subStatus.subscription.expiryDate).toLocaleDateString()}
            <br />
            <button className="choose-btn" onClick={() => navigate("/login", { replace: true })}>
              Continue to Login →
            </button>
          </p>
        ) : (
          <p className="inactive-sub">No active subscription.</p>
        )}
      </div>

      <div className="plans-grid">
        {PLANS.map((p) => (
          <div key={p.id} className={`plan-card ${p.recommended ? "recommended" : ""}`}>
            {p.recommended && <span className="recommended-badge">Recommended</span>}
            <h2>{p.name}</h2>
            <p className="price">{p.price.toLocaleString()} UGX</p>
            <p className="duration">{p.duration}</p>

            <button className="choose-btn" onClick={() => openPlanModal(p)}>
              Choose Plan
            </button>
          </div>
        ))}
      </div>

      {showModal && selectedPlan && (
        <div className="modal-overlay">
          <div className="modal">
            <h2>Mobile Money Payment</h2>

            <form className="modal-form" onSubmit={handlePay}>
              <label>Network</label>
              <select value={network} onChange={(e) => setNetwork(e.target.value)}>
                <option value="mtn">MTN</option>
                <option value="airtel">Airtel</option>
              </select>

              <label>Phone</label>
              <input value={phone} onChange={(e) => setPhone(e.target.value)} required />

              <div className="modal-actions">
                <button type="button" onClick={closeModal}>Cancel</button>
                <button type="submit" disabled={processing}>
                  {processing ? "Processing..." : "Pay & Subscribe"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {popup && (
        <div className="popup-overlay">
          <div className="popup-card">
            <p>{popup.message}</p>
            <button onClick={() => setPopup(null)}>OK</button>
          </div>
        </div>
      )}
    </div>
  );
}