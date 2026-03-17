// src/pages/Fuel/FuelRequestForm.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import "./FuelRequestForm.css";

export default function FuelRequestForm() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    fuelType: "petrol",
    quantityLitres: 10,
    paymentMethod: "cash",
  });

  const [location, setLocation] = useState(null);
  const [place, setPlace] = useState("Fetching place…");
  const [submitting, setSubmitting] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);

  // Fetch GPS
  useEffect(() => {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const coords = {
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
        };
        setLocation(coords);
        reverseLookup(coords.lat, coords.lng);
      },
      () => alert("Enable your location"),
      { enableHighAccuracy: true }
    );
  }, []);

  const reverseLookup = async (lat, lng) => {
    try {
      const res = await axios.get(
        `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`
      );
      setPlace(res.data.display_name || "Unknown place");
    } catch {
      setPlace("Unknown place");
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!location) return alert("Waiting for GPS…");
    setSubmitting(true);

    try {
      const payload = {
        userId: user.id,
        userName: user.name,
        userPhone: user.phone || "",
        fuelType: form.fuelType,
        quantityLitres: Number(form.quantityLitres),
        paymentMethod: form.paymentMethod,
        lat: location.lat,
        lng: location.lng,
        address: place,
      };

      const res = await axios.post(
        "http://localhost:5001/api/fuel/request",
        payload
      );

      navigate(`/fuel/status/${res.data.id}`);
    } catch (err) {
      alert("Failed to create request");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fuel-request-root">
      <div className="fuel-request-card">

        {/* ⭐ Polished Back Button */}
        <button className="back-btn" onClick={() => navigate(-1)}>← Back</button>

        <h2>Request Fuel</h2>

        <form className="fuel-form" onSubmit={submit}>

          <div className="row">
            <label>Fuel Type</label>
            <select
              value={form.fuelType}
              onChange={(e) => setForm({ ...form, fuelType: e.target.value })}
            >
              <option value="petrol">Petrol</option>
              <option value="diesel">Diesel</option>
              <option value="kerosene">Kerosene</option>
            </select>
          </div>

          <div className="row two">
            <div>
              <label>Quantity (L)</label>
              <input
                type="number"
                min="1"
                value={form.quantityLitres}
                onChange={(e) =>
                  setForm({ ...form, quantityLitres: e.target.value })
                }
              />
            </div>

            <div>
              <label>Payment</label>
              <select
                value={form.paymentMethod}
                onChange={(e) =>
                  setForm({ ...form, paymentMethod: e.target.value })
                }
              >
                <option value="cash">Cash</option>
                <option value="momo">Mobile Money</option>
              </select>
            </div>
          </div>

          <div className="location-box">
            <div className="gps">
              {location
                ? `📍 ${location.lat.toFixed(5)}, ${location.lng.toFixed(5)}`
                : "Fetching GPS…"}
            </div>
            <div className="gps-small">{place}</div>
          </div>

          <div className="actions">
            <button className="primary" disabled={submitting}>
              {submitting ? "Requesting…" : "Request Fuel"}
            </button>

            <button
              type="button"
              className="secondary"
              onClick={() => setShowCancelModal(true)}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>

      {/* Cancel Modal */}
      {showCancelModal && (
        <div className="modal-overlay">
          <div className="modal-box">
            <h3>Cancel Fuel Request?</h3>
            <p>Your entered details will be lost.</p>

            <div className="modal-actions">
              <button
                className="modal-cancel"
                onClick={() => setShowCancelModal(false)}
              >
                No, Go Back
              </button>

              <button
                className="modal-confirm"
                onClick={() => navigate("/fuel")}
              >
                Yes, Cancel
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
