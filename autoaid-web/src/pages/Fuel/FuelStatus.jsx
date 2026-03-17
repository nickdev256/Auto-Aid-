// src/pages/Fuel/FuelStatus.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";
import { useParams, useNavigate } from "react-router-dom";
import "./FuelStatus.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function FuelStatus() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [info, setInfo] = useState(null);

  const load = async () => {
    try {
      const res = await axios.get(`${BASE}/api/fuel/track/${id}`);
      setInfo(res.data);
    } catch (err) {
      console.error("Tracking error:", err);
    }
  };

  useEffect(() => {
    load();
    const iv = setInterval(load, 3000);
    return () => clearInterval(iv);
  }, [id]);

  if (!info)
    return (
      <div className="fuel-status-root">
        <div className="loading-box">Loading fuel order…</div>
      </div>
    );

  // STATUS BADGE COLORS
  const getStatusBadge = (status) => {
    switch (status) {
      case "pending":
        return "badge pending";
      case "assigned":
        return "badge assigned";
      case "enroute":
        return "badge enroute";
      case "completed":
        return "badge completed";
      default:
        return "badge unknown";
    }
  };

  return (
    <div className="fuel-status-root">

      <div className="fuel-status-card">

        {/* BACK BUTTON */}
        <button className="back-btn" onClick={() => navigate(-1)}>← Back</button>

        <h2 className="title">Fuel Delivery Status</h2>

        {/* STATUS BADGE */}
        <div className={getStatusBadge(info.status)}>
          {info.status.toUpperCase()}
        </div>

        {/* DETAILS SECTION */}
        <div className="details-box">

          <div className="row">
            <span className="label">Vendor</span>
            <span className="value">
              {info.assignedToName || "Searching for a provider..."}
            </span>
          </div>

          <div className="row">
            <span className="label">Fuel Type</span>
            <span className="value">{info.meta?.fuelType || "—"}</span>
          </div>

          <div className="row">
            <span className="label">Litres</span>
            <span className="value">{info.meta?.quantityLitres || 0} L</span>
          </div>

          <div className="row">
            <span className="label">Payment Method</span>
            <span className="value">{info.meta?.paymentMethod || "—"}</span>
          </div>

        </div>

        {/* ACTION BUTTONS */}
        <div className="action-buttons">

          <button
            className="btn primary"
            onClick={() => navigate(`/vendorMap/${id}`)}
          >
            View Live Map
          </button>

          <button
            className="btn outline"
            onClick={() => navigate(`/provider/chat/${id}`)}
          >
            Chat with Vendor
          </button>

        </div>

        {/* MAP PREVIEW BOX */}
        <div className="map-preview">
          <small>Map preview unavailable — open Live Map for real-time tracking.</small>
        </div>

      </div>

    </div>
  );
}
