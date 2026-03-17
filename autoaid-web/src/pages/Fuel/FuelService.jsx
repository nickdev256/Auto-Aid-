// src/pages/Fuel/FuelService.jsx
import React from "react";
import { useNavigate } from "react-router-dom";
import "./FuelService.css";

export default function FuelService() {
  const nav = useNavigate();

  return (
    <div className="fuel-service-root">

      {/* BACK BUTTON */}
      <button className="back-top" onClick={() => nav("/dashboard")}>
        ← Back to Dashboard
      </button>

      {/* HEADER */}
      <div className="fuel-service-header">
        <h1>⛽ Fuel Delivery</h1>
        <p>Order petrol or diesel delivered to your exact GPS</p>
      </div>

      {/* GRID OF OPTIONS */}
      <div className="fuel-service-grid">

        <div className="fuel-service-card" onClick={() => nav("/fuel/request")}>
          <div className="icon">⛽</div>
          <h3>Request Fuel</h3>
          <p>Order petrol or diesel instantly</p>
        </div>

        <div className="fuel-service-card" onClick={() => nav("/fuel/active")}>
          <div className="icon">🔄</div>
          <h3>Active Request</h3>
          <p>Track your current fuel order</p>
        </div>

        <div className="fuel-service-card" onClick={() => nav("/fuel/history")}>
          <div className="icon">📜</div>
          <h3>Fuel History</h3>
          <p>View all past fuel requests</p>
        </div>

      </div>
    </div>
  );
}
