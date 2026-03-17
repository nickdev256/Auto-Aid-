import React, { useState } from "react";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useNavigate, useLocation } from "react-router-dom";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "./GarageRequest.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function GarageRequest() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [vehicleInfo, setVehicleInfo] = useState("");
  const [issue, setIssue] = useState("");
  const [address, setAddress] = useState("");
  const [coords, setCoords] = useState(null);
  const [placeName, setPlaceName] = useState("Detecting...");
  const [loading, setLoading] = useState(false);

  const getLocation = () => {
    if (!navigator.geolocation) return alert("Your device does not support GPS");
    navigator.geolocation.getCurrentPosition(async (pos) => {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      setCoords({ lat, lng });
      try {
        const res = await axios.get(`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`);
        setPlaceName(res.data.display_name || "Unknown place");
      } catch { setPlaceName("Unknown place"); }

      setTimeout(() => {
        const map = L.map("miniMap").setView([lat, lng], 15);
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png").addTo(map);
        L.marker([lat, lng]).addTo(map);
      }, 150);
    }, () => alert("Failed to capture location."));
  };

  const submitRequest = async () => {
    if (!vehicleInfo) return alert("Enter vehicle details");
    if (!issue) return alert("Describe the issue");
    if (!coords) return alert("Capture location first");
    setLoading(true);
    try {
      await axios.post(`${BASE}/api/garage/request`, {
        userId: user._id, userName: user.name, userPhone: user.phone,
        vehicleInfo, issue, address, lat: coords.lat, lng: coords.lng
      });
      navigate("/garage/active");
    } catch (err) {
      alert("Failed to send request");
    } finally { setLoading(false); }
  };

  return (
    <div className="garage-request-container">
      <button className="back-btn" onClick={() => navigate(-1)}>← Back</button>
      <div className="garage-request-card">
        <h1>Request Garage Assistance</h1>

        <label>Vehicle Info</label>
        <input placeholder="e.g. Toyota Wish, Silver, UBL 123A" value={vehicleInfo} onChange={(e)=>setVehicleInfo(e.target.value)} />

        <label>Issue</label>
        <textarea placeholder="e.g. Engine won't start" value={issue} onChange={(e)=>setIssue(e.target.value)} />

        <label>Address (optional)</label>
        <input placeholder="Optional address" value={address} onChange={(e)=>setAddress(e.target.value)} />

        <div className="loc-row">
          <button className="btn-loc" onClick={getLocation}>Capture My Location</button>
          <div className="place-name">{placeName}</div>
        </div>

        {coords && <div id="miniMap" className="mini-map" />}

        <div className="actions">
          <button className="btn-submit" disabled={loading} onClick={submitRequest}>{loading ? "Sending..." : "Send Request"}</button>
          <button className="btn-ghost" onClick={() => navigate("/garage")}>Cancel</button>
        </div>
      </div>
    </div>
  );
}
