
import React, { useEffect, useState } from "react";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import "./AmbulanceRequestForm.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function AmbulanceRequestForm() {
  const { user } = useAuth();
  const nav = useNavigate();

  const [location, setLocation] = useState(null);
  const [loading, setLoading] = useState(false);

  const [emergencyType, setEmergencyType] = useState("");
  const [condition, setCondition] = useState("");
  const [note, setNote] = useState("");

  const [step, setStep] = useState(1);
  const [gpsError, setGpsError] = useState("");

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [step]);


  const fetchLocation = () => {
    if (!navigator.geolocation) {
      setGpsError("Your device does not support GPS.");
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLocation({
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
        });
        setGpsError("");
      },
      () => setGpsError("Unable to access GPS. Please enable location."),
      { enableHighAccuracy: true }
    );
  };

  useEffect(() => {
    fetchLocation();
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    if (!user) return nav("/login");
    if (!location) return alert("Still obtaining GPS…");

    setLoading(true);

    try {
      const payload = {
        userId: user._id || user.id,
        userName: user.name,
        phone: user.phone || "",
        emergencyType,
        condition,
        note,
        lat: location.lat,
        lng: location.lng,
      };

      const res = await axios.post(`${BASE}/api/ambulance/request`, payload);
      nav(`/ambulance/status/${res.data.id}`);
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.message || "Request failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ambulance-request-root">
      <div className="request-card">

     
        <div className="request-header">
          <h1>🚑 Emergency Ambulance Request</h1>
          <p>Please provide quick details to help responders locate you.</p>
        </div>

        <div className="step-indicator">
          <span className={step === 1 ? "active" : ""}>1</span>
          <span className={step === 2 ? "active" : ""}>2</span>
          <span className={step === 3 ? "active" : ""}>3</span>
        </div>

        <form onSubmit={submit}>
        
          {step === 1 && (
            <div className="section fade">
              <label>Type of Emergency</label>
              <select
                value={emergencyType}
                onChange={(e) => setEmergencyType(e.target.value)}
              >
                <option value="">Select Emergency</option>
                <option value="accident">Accident</option>
                <option value="stroke">Stroke</option>
                <option value="labour">Labour / Pregnancy</option>
                <option value="medical">Medical Emergency</option>
              </select>

              <button
                type="button"
                className="next-btn"
                disabled={!emergencyType}
                onClick={() => setStep(2)}
              >
                Next →
              </button>
            </div>
          )}

          
          {step === 2 && (
            <div className="section fade">
              <label>Patient Condition</label>
              <select
                value={condition}
                onChange={(e) => setCondition(e.target.value)}
              >
                <option value="">Select Condition</option>
                <option value="critical">Critical</option>
                <option value="serious">Serious</option>
                <option value="stable">Stable</option>
              </select>

              <label>Additional Notes (optional)</label>
              <textarea
                placeholder="Example: Patient unconscious after collision…"
                value={note}
                onChange={(e) => setNote(e.target.value)}
              />

              <div className="actions-row">
                <button type="button" onClick={() => setStep(1)}>
                  ← Back
                </button>

                <button
                  type="button"
                  className="next-btn"
                  disabled={!condition}
                  onClick={() => setStep(3)}
                >
                  Next →
                </button>
              </div>
            </div>
          )}

       
          {step === 3 && (
            <div className="section fade">
              <h3>📍 Your Location</h3>

              {gpsError && <p className="gps-error">{gpsError}</p>}

              {location ? (
                <div className="gps-box">
                  <p>Latitude: <strong>{location.lat.toFixed(5)}</strong></p>
                  <p>Longitude: <strong>{location.lng.toFixed(5)}</strong></p>
                </div>
              ) : (
                <p className="gps-loading">Obtaining GPS…</p>
              )}

              <button type="button" className="refresh-btn" onClick={fetchLocation}>
                Refresh GPS
              </button>

              <div className="actions-row">
                <button type="button" onClick={() => setStep(2)}>
                  ← Back
                </button>

                <button className="request-btn" type="submit" disabled={loading}>
                  {loading ? "🚑 Dispatching…" : "Request Ambulance"}
                </button>
              </div>
            </div>
          )}
        </form>
      </div>
    </div>
  );
}
