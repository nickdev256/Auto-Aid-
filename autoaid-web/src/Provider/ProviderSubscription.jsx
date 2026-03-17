// src/Provider/ProviderSubscription.jsx
import React, { useEffect, useState } from "react";
import {
  getProviderSubscriptionStatus,
  startSubscriptionPayment,
} from "../services/api";
import "../provider/ProviderSubscription.css";
import { useNavigate } from "react-router-dom";


const PLANS = [
  { id: "monthly", name: "Monthly", price: 50000, duration: "30 days", recommended: false },
  { id: "quarterly", name: "Quarterly", price: 100000, duration: "90 days", recommended: true },
  { id: "yearly", name: "Yearly", price: 250000, duration: "365 days", recommended: false },
];

export default function ProviderSubscription() {
  const navigate = useNavigate();

  // ✅ provider comes from OTP step (NOT auth context)
  const providerId = localStorage.getItem("pendingProviderId");

  const [subStatus, setSubStatus] = useState(null);
  const [loading, setLoading] = useState(true);

  const [showModal, setShowModal] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState(null);

  const [phone, setPhone] = useState("");
  const [network, setNetwork] = useState("mtn");
  const [processing, setProcessing] = useState(false);

  const [popup, setPopup] = useState(null);

  // ✅ block access if user didn't come from OTP
  useEffect(() => {
    if (!providerId) {
      navigate("/signup", { replace: true });
    }
  }, [providerId, navigate]);

  // Load subscription
  const loadStatus = async () => {
    if (!providerId) return;

    try {
      const data = await getProviderSubscriptionStatus(providerId);
      setSubStatus(data);
    } catch (err) {
      console.error("Subscription status error:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStatus();
    // eslint-disable-next-line
  }, [providerId]);

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

  // ✅ after payment -> LOGIN
  const handlePay = async (e) => {
    e.preventDefault();

    if (!phone) {
      return setPopup({ type: "info", message: "Enter phone number" });
    }

    if (!selectedPlan?.id) {
      return setPopup({ type: "info", message: "Select a plan first" });
    }

    setProcessing(true);

    try {
      const res = await startSubscriptionPayment({
        providerId,
        planId: selectedPlan.id,
        phone,
        network,
      });

      // ✅ if your backend confirms instantly (sandbox or real)
      if (res?.success || res?.sandboxMode) {
        setPopup({
          type: "success",
          message: "Subscription activated successfully. Please login.",
        });

        // clear signup temp storage
        localStorage.removeItem("pendingProviderId");
        localStorage.removeItem("pendingProviderEmail");
        localStorage.removeItem("pendingEmail");

        closeModal();

        setTimeout(() => {
          navigate("/login", { replace: true });
        }, 1200);

        return;
      }

      // If payment is async (prompt on phone)
      setPopup({
        type: "info",
        message: "Payment started. Approve on your phone. You will login after activation.",
      });

      closeModal();

      // Optional: try refresh status for a short time then redirect to login
      setTimeout(async () => {
        await loadStatus();

        // If active now -> login
        const active = subStatus?.subscription?.active === true;
        if (active) {
          localStorage.removeItem("pendingProviderId");
          localStorage.removeItem("pendingProviderEmail");
          localStorage.removeItem("pendingEmail");
          navigate("/login", { replace: true });
        }
      }, 6000);
    } catch (err) {
      setPopup({
        type: "info",
        message: err?.response?.data?.message || err.message || "Payment failed. Try again.",
      });
    } finally {
      setProcessing(false);
    }
  };

  if (loading) return <p>Loading...</p>;

  const isActive = subStatus?.subscription?.active;

  return (
    <div className="subscription-page">
      {/* ✅ No back-to-dashboard (because provider not logged in yet) */}
      <button className="back-btn" onClick={() => navigate("/otp")}>
        ← Back
      </button>

      <h1 className="page-title">Subscription Plans</h1>

      {/* CURRENT SUBSCRIPTION */}
      <div className="current-sub-box">
        <h3>Current Subscription</h3>

        {isActive ? (
          <p className="active-sub">
            You are on <strong>{subStatus.subscription.plan}</strong>{" "}
            <span className="active-pill">ACTIVE</span>
            <br />
            Expires: {new Date(subStatus.subscription.expiryDate).toLocaleDateString()}
            <br />
            <button
              className="choose-btn"
              style={{ marginTop: 10 }}
              onClick={() => navigate("/login", { replace: true })}
            >
              Continue to Login →
            </button>
          </p>
        ) : (
          <p className="inactive-sub">No active subscription.</p>
        )}
      </div>

      {/* PLAN CARDS */}
      <div className="plans-grid">
        {PLANS.map((p) => (
          <div key={p.id} className={`plan-card ${p.recommended ? "recommended" : ""}`}>
            {p.recommended && <span className="recommended-badge">Recommended</span>}

            <h2>{p.name}</h2>
            <p className="price">{p.price.toLocaleString()} UGX</p>
            <p className="duration">{p.duration}</p>

            <ul className="features">
              <li>Full Visibility</li>
              <li>Priority Ranking</li>
              <li>Real-time Notifications</li>
            </ul>

            <button className="choose-btn" onClick={() => openPlanModal(p)}>
              Choose Plan
            </button>
          </div>
        ))}
      </div>

      {/* PAYMENT MODAL */}
      {showModal && selectedPlan && (
        <div className="modal-overlay">
          <div className="modal">
            <h2>Mobile Money Payment</h2>

            <p className="modal-plan">
              <strong>{selectedPlan.name}</strong> — {selectedPlan.price.toLocaleString()} UGX
            </p>

            <form className="modal-form" onSubmit={handlePay}>
              <label>Choose Network</label>

              <div className="network-select">
                <label className="network-option">
                  <input
                    type="radio"
                    value="mtn"
                    checked={network === "mtn"}
                    onChange={(e) => setNetwork(e.target.value)}
                  />
                  <img src="/images/mtn.png" className="network-icon" alt="MTN" /> MTN Mobile Money
                </label>

                <label className="network-option">
                  <input
                    type="radio"
                    value="airtel"
                    checked={network === "airtel"}
                    onChange={(e) => setNetwork(e.target.value)}
                  />
                  <img src="/images/airtel.png" className="network-icon" alt="Airtel" /> Airtel Money
                </label>
              </div>

              <label>Phone Number</label>
              <input
                type="tel"
                placeholder="0772xxxxxx"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                required
              />

              <div className="modal-actions">
                <button type="button" className="cancel-btn" disabled={processing} onClick={closeModal}>
                  Cancel
                </button>

                <button type="submit" className="pay-btn" disabled={processing}>
                  {processing ? "Processing..." : "Pay & Subscribe"}
                </button>
              </div>
            </form>

            <p className="momo-note">
              You will receive a prompt on your phone to approve the payment.
            </p>
          </div>
        </div>
      )}

      {/* POPUP MESSAGE */}
      {popup && (
        <div className={`popup-overlay ${popup.type}`}>
          <div className="popup-card">
            <h3>{popup.type === "success" ? "✔ Success" : "ℹ Information"}</h3>
            <p>{popup.message}</p>

            <button className="popup-btn" onClick={() => setPopup(null)}>
              OK
            </button>
          </div>
        </div>
      )}
    </div>
  );
}