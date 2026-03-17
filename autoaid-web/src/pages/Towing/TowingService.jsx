import React from "react";
import { useNavigate } from "react-router-dom";
import "./TowingService.css";

export default function TowingService(){
  const nav = useNavigate();
  return (
    <div className="towing-service-root">
      <div className="towing-card">
        <button className="back" onClick={() => nav(-1)}>⬅ Back</button>
        <h1>Towing Service</h1>
        <p className="muted">Fast tow help — flatbed & standard options</p>

        <div className="towing-actions">
          <button className="primary" onClick={() => nav("/towing/request")}>🚨 Request Tow</button>
          <button className="ghost" onClick={() => nav("/towing/history")}>📜 History</button>
          <button className="ghost" onClick={() => nav("/towing/active")}>🔎 Active Request</button>
        </div>
      </div>
    </div>
  );
}
