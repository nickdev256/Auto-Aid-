import React, { useEffect, useState } from "react";
import ServiceCard from "../../components/ServiceCard";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import axios from "axios";

import "./Dashboard.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [recent, setRecent] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showLogoutModal, setShowLogoutModal] = useState(false);

  const confirmLogout = () => {
    logout();
    navigate("/");
  };

  const handleFuelClick = () => {
    if (user) navigate("/fuel");
    else navigate("/signup", { state: { redirectTo: "/dashboard" } });
  };

  useEffect(() => {
    if (!user) return;
    loadRecent();
  }, [user]);

  // 🟢 FIXED RECENT REQUEST FETCHING
  const loadRecent = async () => {
    try {
      const userId = user._id;

      const endpoints = [
        `${BASE}/api/garage/history/${userId}`,
        `${BASE}/api/fuel/history/${userId}`,
        `${BASE}/api/towing/history/${userId}`,
        `${BASE}/api/ambulance/history/${userId}`,
      ];

      const results = await Promise.allSettled(
        endpoints.map((ep) => axios.get(ep))
      );

      let all = [];

      results.forEach((r) => {
        if (
          r.status === "fulfilled" &&
          Array.isArray(r.value.data)
        ) {
          all.push(...r.value.data);
        }
      });

      all.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      setRecent(all.slice(0, 5));
    } catch (err) {
      console.error("Recent load error:", err);
    }

    setLoading(false);
  };

  return (
    <div className="dash-container animated-fade">

      {/* HEADER */}
      <header className="dash-header">
        <div className="header-left">
          <h2 className="welcome">Welcome to AutoAid {user?.name || "User"} 👋</h2>
          <p className="subtitle">Your trusted roadside assistant</p>
        </div>

        <div className="profile-card">
          <div className="profile-avatar">
            {user?.name?.[0]?.toUpperCase() || "U"}
          </div>

          <div className="profile-info">
            <div className="profile-name">{user?.name}</div>
            <div className="profile-role">User • Online</div>
          </div>

          <button
            className="logout-small"
            onClick={() => setShowLogoutModal(true)}
          >
            Logout
          </button>
        </div>
      </header>

      {/* QUICK ACTIONS */}
      <div className="quick-actions">
        <div className="quick-action improved" onClick={() => navigate("/garage")}>
          <div className="qa-icon">🛠️</div>
          <h4>Garage Support</h4>
          <p className="qa-desc">On-site repair and diagnostics.</p>
          <button className="qa-btn">Request</button>
        </div>

        <div className="quick-action improved" onClick={handleFuelClick}>
          <div className="qa-icon">⛽</div>
          <h4>Fuel Delivery</h4>
          <p className="qa-desc">Fuel delivered to your location.</p>
          <button className="qa-btn">Order Fuel</button>
        </div>

        <div className="quick-action improved" onClick={() => navigate("/towing")}>
          <div className="qa-icon">🚚</div>
          <h4>Towing</h4>
          <p className="qa-desc">Fast & reliable towing service.</p>
          <button className="qa-btn">Tow Now</button>
        </div>

        <div className="quick-action improved" onClick={() => navigate("/ambulance")}>
          <div className="qa-icon">🚑</div>
          <h4>Ambulance</h4>
          <p className="qa-desc">Emergency medical assistance.</p>
          <button className="qa-btn">Call Ambulance</button>
        </div>
      </div>

      {/* RECENT REQUESTS */}
      <section className="history-section">
        <h3 className="section-title">Recent Requests</h3>

        <div className="history-box">
          {loading ? (
            <>
              <div className="skeleton-box skeleton"></div>
              <div className="skeleton-box skeleton"></div>
              <div className="skeleton-box skeleton"></div>
            </>
          ) : recent.length === 0 ? (
            <p>No recent requests yet.</p>
          ) : (
            <ul className="history-list">
              {recent.map((req, i) => (
                <li key={i} className="history-item">
                  <div className="history-row">
                    <span className="req-service">
                      {req.serviceType?.toUpperCase()}
                    </span>
                    <span className="req-time">
                      {new Date(req.createdAt).toLocaleString()}
                    </span>
                  </div>
                  <div className="req-status">
                    Status: <strong>{req.status}</strong>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      {/* LOGOUT MODAL */}
      {showLogoutModal && (
        <div className="modal-overlay" onClick={() => setShowLogoutModal(false)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <h3>Logout Confirmation</h3>
            <p>Are you sure you want to log out?</p>

            <div className="modal-actions">
              <button className="modal-cancel" onClick={() => setShowLogoutModal(false)}>
                Cancel
              </button>

              <button className="modal-logout" onClick={confirmLogout}>
                Logout
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
