// src/pages/Fuel/FuelHistory.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import "./FuelHistory.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function FuelHistory() {
  const { user } = useAuth();
  const nav = useNavigate();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;

    axios
      .get(`${BASE}/api/fuel/history/${user.id || user._id}`)
      .then((res) => setList(res.data || []))
      .catch(() => setList([]))
      .finally(() => setLoading(false));
  }, [user]);

  if (loading)
    return (
      <div className="fuel-history-root">
        <p className="loading">Loading…</p>
      </div>
    );

  return (
    <div className="fuel-history-root">
      
      <div className="history-container">

        {/* Modern Back Button */}
        <button className="back-btn" onClick={() => nav(-1)}>
          ← Back
        </button>

        <h2 className="title">Fuel Request History</h2>

        {!list.length ? (
          <div className="empty-box">
            <p>No fuel requests made yet.</p>
            <button className="primary" onClick={() => nav("/fuel/request")}>
              Request Fuel
            </button>
          </div>
        ) : (
          <div className="history-list">
            {list.map((r) => (
              <div key={r.requestId} className="history-card-item">

                <div className="history-info">
                  <div className="h-type">{r.meta?.fuelType}</div>
                  <div className="h-qty">{r.meta?.quantityLitres} L</div>

                  <div className={`h-status status-${r.status.toLowerCase()}`}>
                    {r.status}
                  </div>

                  <div className="h-date">
                    {new Date(r.createdAt).toLocaleString()}
                  </div>
                </div>

                <div className="history-actions">
                  <button 
                    className="view-btn"
                    onClick={() => nav(`/fuel/status/${r.requestId}`)}
                  >
                    View Details →
                  </button>
                </div>

              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
