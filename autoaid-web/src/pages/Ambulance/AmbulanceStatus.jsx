import React, { useEffect, useState } from "react";
import axios from "axios";
import { useParams, useNavigate } from "react-router-dom";
import "./AmbulanceStatus.css";

export default function AmbulanceStatus() {
  const { id } = useParams();
  const nav = useNavigate();
  const [details, setDetails] = useState(null);

  const load = async () => {
    try {
      const res = await axios.get(`http://localhost:5001/api/ambulance/track/${id}`);
      setDetails(res.data);
    } catch (err) {
      console.error("Tracking error:", err);
    }
  };

  useEffect(() => {
    load();
    const interval = setInterval(load, 3000);
    return () => clearInterval(interval);
  }, []);

  if (!details) return <p className="loading">Loading...</p>;

  const timelineSteps = [
    "pending",
    "assigned",
    "arrived",
    "transporting",
    "completed"
  ];

  return (
    <div className="status-root">
      <div className="glass-card">

  
        <button className="back-btn" onClick={() => nav(-1)}>← Back</button>

        <h2>🚑 Ambulance Status</h2>

        {/* STATUS BADGE */}
        <div className={`status-badge status-${details.status}`}>
          {details.status.toUpperCase()}
        </div>

   
        <div className="info-card">
          <h3>Assigned Ambulance</h3>
          {details.assignedToName ? (
            <>
              <p><strong>Driver:</strong> {details.assignedToName}</p>
              <p className="eta">Estimated Arrival: 3–7 minutes</p>
            </>
          ) : (
            <p>Searching for available ambulance...</p>
          )}
        </div>

        {/* LIVE TIMELINE */}
        <div className="timeline">
          {timelineSteps.map((step, i) => {
            const active = step === details.status;
            const completed = timelineSteps.indexOf(details.status) > i;

            return (
              <div key={i} className={`timeline-step 
                ${active ? "active" : ""}
                ${completed ? "completed" : ""}
              `}>
                <span className="dot"></span>
                <p>{step}</p>
              </div>
            );
          })}
        </div>

    
        <button
          className="map-btn"
          onClick={() => nav(`/ambulance/map/${id}`)}
        >
          View Live Map
        </button>

        <button className="refresh-btn" onClick={load}>⟳ Refresh</button>

      </div>
    </div>
  );
}
