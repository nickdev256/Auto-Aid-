// src/pages/Towing/TowingStatus.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "./TowingStatus.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function TowingStatus() {
  const { id } = useParams();
  const nav = useNavigate();
  const { user } = useAuth();

  const [r, setR] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    if (!id) {
      console.error("Missing towing request ID");
      setLoading(false);
      return;
    }

    try {
      const res = await axios.get(`${BASE}/api/towing/${id}`);
      setR(res.data);
    } catch (err) {
      console.error("Load status error:", err);
      setR(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    const iv = setInterval(load, 5000);
    return () => clearInterval(iv);
  }, [id]);

  // Correct backend statuses mapped to steps
  const statusToStep = (status) => {
    const map = {
      pending: 1,
      assigned: 2,
      on_way: 3,
      "on way": 3,
      arrived: 4,
      completed: 5,
      cancelled: -1,
    };
    return map[(status || "").toLowerCase()] || 1;
  };

  if (loading)
    return (
      <div className="ts-root">
        <div className="ts-card ts-loading">Loading towing status…</div>
      </div>
    );

  if (!r)
    return (
      <div className="ts-root">
        <div className="ts-card">
          <button className="ts-back" onClick={() => nav(-1)}>
            ← Back
          </button>
          <div className="ts-empty">Towing request not found</div>
        </div>
      </div>
    );

  const step = statusToStep(r.status);

  return (
    <div className="ts-root">
      <div className="ts-card">

        {/* TOP BAR */}
        <div className="ts-top">
          <button className="ts-back" onClick={() => nav(-1)}>
            ← Back
          </button>

          <div className="ts-title-group">
            <h1 className="ts-title">Towing Status</h1>
            <div className={`ts-pill ${r.status?.replace(/\s+/g, "").toLowerCase()}`}>
              {r.status?.toUpperCase()}
            </div>
          </div>

          <div className="ts-actions">
            <button className="btn-outline" onClick={() => nav("/towing")}>
              Back to Towing
            </button>

            {r.assignedTo && (
              <button className="btn-primary" onClick={() => nav(`/towing/map/${id}`)}>
                Track Tow
              </button>
            )}
          </div>
        </div>

        {/* PROGRESS STEPS */}
        <div className="ts-steps">
          <div className={`step ${step >= 1 ? "active" : ""}`}>
            <div className="num">1</div>
            <div className="label">Pending</div>
          </div>

          <div className={`step ${step >= 2 ? "active" : ""}`}>
            <div className="num">2</div>
            <div className="label">Assigned</div>
          </div>

          <div className={`step ${step >= 3 ? "active" : ""}`}>
            <div className="num">3</div>
            <div className="label">On the Way</div>
          </div>

          <div className={`step ${step >= 4 ? "active" : ""}`}>
            <div className="num">4</div>
            <div className="label">Arrived</div>
          </div>

          <div className={`step ${step >= 5 ? "active" : ""}`}>
            <div className="num">5</div>
            <div className="label">Completed</div>
          </div>
        </div>

        {/* MAIN BODY */}
        <div className="ts-body">

          {/* LEFT SIDE DETAILS */}
          <div className="ts-left">

            <div className="info-card">
              <div className="info-row">
                <div className="info-label">User</div>
                <div className="info-value">{r.userName}</div>
              </div>

              <div className="info-row">
                <div className="info-label">Vehicle</div>
                <div className="info-value">{r.vehicleInfo || "—"}</div>
              </div>

              <div className="info-row">
                <div className="info-label">Tow Type</div>
                <div className="info-value">{r.meta?.towType || "standard"}</div>
              </div>

              <div className="info-row">
                <div className="info-label">Assigned To</div>
                <div className="info-value">{r.assignedToName || "Not assigned"}</div>
              </div>

              <div className="info-row">
                <div className="info-label">Location</div>
                <div className="info-value">
                  {`${r.lat?.toFixed(5)}, ${r.lng?.toFixed(5)}`}
                </div>
              </div>

              <div className="info-row">
                <div className="info-label">Created</div>
                <div className="info-value">{new Date(r.createdAt).toLocaleString()}</div>
              </div>
            </div>

            {/* PROVIDER INFO */}
            <div className="provider-card">
              <div className="provider-left">
                <div className="avatar">
                  {(r.assignedToName || "??").slice(0, 2).toUpperCase()}
                </div>
                <div>
                  <div className="provider-name">
                    {r.assignedToName || "No provider assigned"}
                  </div>
                  <div className="provider-meta">
                    {r.assignedTo ? "Assigned provider" : "Waiting for assignment"}
                  </div>
                </div>
              </div>

              <div className="provider-actions">
                {r.assignedTo ? (
                  <>
                    <button
                      className="btn-primary"
                      onClick={() => nav(`/towing/map/${id}`)}
                    >
                      Open Map
                    </button>

                    <button
                      className="btn-outline"
                      onClick={() => nav(`/provider/chat/${id}`)}
                    >
                      Message Provider
                    </button>
                  </>
                ) : (
                  <button className="btn-ghost" disabled>
                    Awaiting assignment…
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* RIGHT SIDE MAP & NOTES */}
          <div className="ts-right">
            <div className="map-card">
              <img src="/images/map-preview.png" alt="map preview" className="map-img" />
              <div className="map-overlay">
                <div className="map-caption">Pickup Location</div>
                {r.assignedTo && <div className="map-eta">ETA: {r.eta || "—"}</div>}
              </div>
            </div>

            <div className="notes-card">
              <h4>Notes</h4>
              <p className="muted">
                {r.problemDescription || r.issue || "No additional notes provided."}
              </p>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
