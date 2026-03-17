// src/Provider/ProviderDashboard.jsx
import React, { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import BusinessSettings from "./BusinessSettings";
import "./ProviderDashboard.css";

export default function ProviderDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [subscriptionInfo, setSubscriptionInfo] = useState(null);
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // NEW MODAL STATE
  const [showSubscribeModal, setShowSubscribeModal] = useState(false);

  // Safely resolve provider id
  const providerId = user?._id || user?.id;

  // -----------------------------
  // FETCH SUBSCRIPTION INFO
  // -----------------------------
  const loadSubscription = async () => {
    try {
      const res = await axios.get("http://localhost:5001/api/admin/users");
      const users = res.data || [];
      const me = users.find((u) => u._id === providerId || u.id === providerId);

      if (me && me.subscription) {
        setSubscriptionInfo(me.subscription);
      } else {
        setSubscriptionInfo({ active: false, plan: null });
      }
    } catch (err) {
      console.error("Subscription Error:", err);
      setSubscriptionInfo({ active: false, plan: null });
    }
  };

  // -----------------------------
  // FETCH REQUESTS (View always allowed)
  // -----------------------------
  const loadRequests = async () => {
    if (!providerId) return;

    try {
      const res = await axios.get(
        `http://localhost:5001/api/garage/byProvider/${providerId}`
      );
      setRequests(res.data || []);
    } catch (err) {
      console.error("Failed to fetch requests:", err);
    }
  };

  // INITIAL LOAD
  useEffect(() => {
    if (!user || !providerId) return;

    const init = async () => {
      await loadSubscription();
      await loadRequests(); // now loads even if not subscribed
      setLoading(false);
    };

    init();
  }, [providerId]);

  // AUTO FETCH REQUESTS IF SUBSCRIBED ONLY
  useEffect(() => {
    if (!subscriptionInfo?.active) return;

    const interval = setInterval(loadRequests, 5000);
    return () => clearInterval(interval);
  }, [subscriptionInfo?.active]);

  // ACCEPT REQUEST WITH CHECK
  const acceptRequest = async (id) => {
    if (!subscriptionInfo?.active) {
      setShowSubscribeModal(true);
      return;
    }

    try {
      await axios.post(
        `http://localhost:5001/api/garage/${id}/assign`,
        { providerId }
      );
      loadRequests();
    } catch (err) {
      alert(err.response?.data?.message || "Failed to accept request");
    }
  };

  // COMPLETE REQUEST
  const completeRequest = async (id) => {
    try {
      await axios.patch(
        `http://localhost:5001/api/garage/${id}/status`,
        { status: "completed" }
      );
      loadRequests();
    } catch (err) {
      alert("Failed to mark completed");
    }
  };

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  if (loading) return <p>Loading dashboard...</p>;

  const isSubscribed = subscriptionInfo?.active === true;

  return (
    <div className="provider-dashboard-container">
      <h1>Provider Dashboard</h1>
      <h2>Welcome, {user?.name}</h2>

      {/* SUBSCRIPTION BAR */}
      {!isSubscribed && (
        <div className="subscription-warning">
          ⚠️ Your subscription is <strong>inactive</strong>. <br />
          You can view customer requests but cannot accept until you subscribe.
          <button
            className="subscribe-btn"
            onClick={() => navigate("/provider/subscription-info")}
          >
            View Subscription Plans
          </button>
        </div>
      )}

      {isSubscribed && (
        <div className="subscription-bar">
          <p>
            Subscription:{" "}
            <strong>{subscriptionInfo.plan || "Unknown plan"}</strong>
          </p>
        </div>
      )}

      {/* BUSINESS PROFILE */}
      <section className="provider-section">
        <h2>Your Business Profile</h2>
        <BusinessSettings />
      </section>

      {/* REQUESTS (Visible Always) */}
      <h2>Incoming Requests</h2>

      {requests.length === 0 ? (
        <p>No requests yet. Keep your app open.</p>
      ) : (
        <div className="jobs-grid">
          {requests.map((req) => {
            const reqId = req.requestId || req.id || req._id;

            return (
              <div key={reqId} className="job-card">
                <h3>{req.serviceType}</h3>
                <p><strong>User:</strong> {req.userName}</p>
                <p><strong>Phone:</strong> {req.userPhone}</p>
                <p><strong>Address:</strong> {req.address}</p>
                <p><strong>Status:</strong> {req.status}</p>

                {req.status === "pending" && (
                  <button
                    onClick={() => acceptRequest(reqId)}
                    disabled={!isSubscribed}
                    className={!isSubscribed ? "disabled-btn" : ""}
                  >
                    Accept
                  </button>
                )}

                <div className="provider-actions">
                  <button
                    onClick={() => navigate(`/provider/details/${reqId}`)}
                    className="details-btn"
                  >
                    View Details
                  </button>

                  <button
                    onClick={() => navigate(`/provider/map/${reqId}`)}
                    className="map-btn"
                  >
                    Navigate
                  </button>

                  <button
                    onClick={() => navigate(`/provider/chat/${reqId}`)}
                    className="chat-btn"
                  >
                    Chat
                  </button>

                  {req.status === "assigned" && (
                    <button
                      onClick={() => completeRequest(reqId)}
                      className="complete-btn"
                    >
                      Mark Completed
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* 🔥 SUBSCRIPTION REQUIRED MODAL */}
      {showSubscribeModal && (
        <div className="modal-overlay">
          <div className="modal-box">
            <h2>Subscription Required</h2>
            <p>You need an active subscription to accept customer requests.</p>
            <button
              className="primary-btn"
              onClick={() => navigate("/provider/subscription")}
            >
              View Plans & Subscribe
            </button>
            <button
              className="secondary-btn"
              onClick={() => setShowSubscribeModal(false)}
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
