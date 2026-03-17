// src/pages/Fuel/FuelActive.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import "./FuelActive.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function FuelActive() {
  const { user } = useAuth();
  const nav = useNavigate();
  const [active, setActive] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;

    axios
      .get(`${BASE}/api/fuel/latest/${user.id || user._id}`)
      .then((res) => setActive(res.data))
      .catch(() => setActive(null))
      .finally(() => setLoading(false));
  }, [user]);

  if (loading) return <div className="fuel-root"><p>Loading…</p></div>;

  return (
    <div className="fuel-active-root">

      <div className="fuel-active-card">

        {/* ⭐ Modern Back Button */}
        <button className="back-btn" onClick={() => nav(-1)}>← Back</button>

        <h2 className="title">Active Fuel Request</h2>

        {!active ? (
          <div className="empty-state">
            <p>No active fuel requests</p>
            <button className="primary" onClick={() => nav("/fuel/request")}>
              Request Fuel
            </button>
          </div>
        ) : (
          <div className="active-box">

            <div className="info-row">
              <strong>Fuel Type:</strong> {active.meta?.fuelType}
            </div>
            <div className="info-row">
              <strong>Litres:</strong> {active.meta?.quantityLitres} L
            </div>
            <div className="info-row">
              <strong>Payment:</strong> {active.meta?.paymentMethod}
            </div>
            <div className="info-row">
              <strong>Status:</strong> {active.status}
            </div>

            <div className="actions">
              <button
                className="primary"
                onClick={() => nav(`/fuel/status/${active.requestId}`)}
              >
                View Status
              </button>

              {active.assignedTo && (
                <button
                  className="secondary"
                  onClick={() => nav(`/vendorMap/${active.requestId}`)}
                >
                  Track Vendor
                </button>
              )}
            </div>
          </div>
        )}
      </div>

    </div>
  );
}
