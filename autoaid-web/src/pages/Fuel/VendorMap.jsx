// src/pages/Fuel/FuelMap.jsx (VendorMap)
import React from "react";
import { useNavigate } from "react-router-dom";
import "./VendorMap.css";

function VendorMap({ request, providerLocation }) {
  const nav = useNavigate();

  return (
    <div className="vendor-map-root">
      <div className="vendor-map-card">
        <button className="back" onClick={() => nav(-1)}>← Back</button>
        <h2>Vendor Tracking</h2>

        <div className="map-top">
          <div className="info">
            <div><strong>Fuel:</strong> {request?.fuelType || "—"}</div>
            <div><strong>Qty:</strong> {request?.quantityLitres || "—"} L</div>
            <div><strong>Location:</strong> {request?.address || "GPS"}</div>
          </div>

          <div className="vendor-status">
            <div className="dot" />
            <div>
              <div className="status-title">{providerLocation ? "Vendor Online" : "Waiting for Vendor"}</div>
              <div className="small-muted">{providerLocation ? `${providerLocation.lat.toFixed(5)}, ${providerLocation.lng.toFixed(5)}` : "Not assigned yet"}</div>
            </div>
          </div>
        </div>

        <div className="map-canvas">
          <div className="map-placeholder">
            <p>Map will be shown here (Google Maps / Leaflet)</p>
            {providerLocation && <div className="pin">📍 Vendor</div>}
          </div>
        </div>

        <div className="actions">
          <button className="primary" onClick={() => alert("Open navigation (native) — implement if desired")}>Open Navigation</button>
          <button className="secondary" onClick={() => nav("/fuel")}>Back to Fuel</button>
        </div>
      </div>
    </div>
  );
}

export default VendorMap;
