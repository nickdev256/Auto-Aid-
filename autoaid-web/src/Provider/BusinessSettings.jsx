// src/Provider/ProviderSettings.jsx
import React, { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { updateProviderBusiness } from "../services/api";
import { useNavigate } from "react-router-dom";
import "./BusinessSettings.css";

export default function ProviderSettings() {
  const { user, setUser } = useAuth();
  const navigate = useNavigate();

  if (!user) return <p>You must be logged in as a provider.</p>;

  /* =======================================================
        INITIAL FORM STATE
  ======================================================= */
  const [form, setForm] = useState({
    businessName: user.businessName || "",
    phone: user.phone || "",
    address: user.address || "",
    businessType: user.businessType || "",
    services: user.services || [],

    description: user.description || "",
    serviceRadiusKm: user.serviceRadiusKm || 10,
    isOnline: user.isOnline ?? true,

    basePrice: user.basePrice || 0,
    pricePerKm: user.pricePerKm || 0,

    openTime: user.openTime || "",
    closeTime: user.closeTime || "",

    paymentMethod: user.paymentMethod || "mtn",

    lat: user.lat || null,
    lng: user.lng || null,

    logo: null,
    licenseFile: null,
  });

  const [saving, setSaving] = useState(false);

  /* =======================================================
       GPS LOCATION UPDATE
  ======================================================= */
  const updateLocation = () => {
    if (!navigator.geolocation)
      return alert("Your device does not support GPS.");

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setForm({
          ...form,
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
        });
        alert("📍 GPS Location Updated!");
      },
      () => alert("Enable GPS permissions to update location."),
      { enableHighAccuracy: true }
    );
  };

  /* =======================================================
       SAVE CHANGES TO BACKEND
  ======================================================= */
  const saveChanges = async (e) => {
    e.preventDefault();
    setSaving(true);

    const payload = { ...form };
    delete payload.logo;
    delete payload.licenseFile;

    try {
      const res = await updateProviderBusiness(user.id || user._id, payload);

      const updatedUser = { ...user, ...res };
      setUser(updatedUser);
      localStorage.setItem("autoaid_user", JSON.stringify(updatedUser));

      alert("Settings updated successfully! 🎉");
    } catch (err) {
      alert("❌ Update failed: " + err.message);
    }

    setSaving(false);
  };

  /* =======================================================
        MULTI-SERVICE SELECT HANDLER
  ======================================================= */
  const toggleService = (service) => {
    const selected = form.services.includes(service)
      ? form.services.filter((s) => s !== service)
      : [...form.services, service];

    setForm({ ...form, services: selected });
  };

  return (
    <div className="provider-settings">

      {/* BACK BUTTON */}
      <button className="back-btn" onClick={() => navigate("/provider/dashboard")}>
        ← Back to Dashboard
      </button>

      {/* HEADER */}
      <div className="settings-header">
        <h1>Provider Business Settings</h1>
        <p>Update your business details & service preferences.</p>

        <div className={`verify-status ${user.isVerified ? "verified" : "pending"}`}>
          {user.isVerified ? "✔ Verified Provider" : "⏳ Pending Verification"}
        </div>
      </div>

      {/* MAIN CARD */}
      <div className="settings-card">
        <form className="settings-form" onSubmit={saveChanges}>

          {/* =======================================================
              BUSINESS NAME + PHONE
          ======================================================= */}
          <div className="two-col">
            <div className="form-group">
              <label>Business Name</label>
              <input
                value={form.businessName}
                onChange={(e) => setForm({ ...form, businessName: e.target.value })}
                placeholder="Enter your business name"
              />
            </div>

            <div className="form-group">
              <label>Phone Number</label>
              <input
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                placeholder="+256 700 000000"
              />
            </div>
          </div>

          {/* =======================================================
              ADDRESS + BUSINESS TYPE
          ======================================================= */}
          <div className="two-col">
            <div className="form-group">
              <label>Address</label>
              <input
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
                placeholder="Business address"
              />
            </div>

            <div className="form-group">
              <label>Primary Business Type</label>
              <select
                value={form.businessType}
                onChange={(e) => setForm({ ...form, businessType: e.target.value })}
              >
                <option value="">Select service type</option>
                <option value="garage">Garage / Mechanic</option>
                <option value="fuel">Fuel Delivery</option>
                <option value="towing">Towing</option>
                <option value="ambulance">Ambulance</option>
              </select>
            </div>
          </div>

        
          {/* =======================================================
              DESCRIPTION
          ======================================================= */}
          <div className="form-group">
            <label>Description</label>
            <textarea
              rows={3}
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="Describe your service"
            />
          </div>

          {/* =======================================================
              PRICING
          ======================================================= */}
          <h3 className="section-label">Pricing</h3>

          <div className="two-col">
            <div className="form-group">
              <label>Base Price (UGX)</label>
              <input
                type="number"
                value={form.basePrice}
                onChange={(e) => setForm({ ...form, basePrice: Number(e.target.value) })}
              />
            </div>

            <div className="form-group">
              <label>Price Per KM (UGX)</label>
              <input
                type="number"
                value={form.pricePerKm}
                onChange={(e) => setForm({ ...form, pricePerKm: Number(e.target.value) })}
              />
            </div>
          </div>

          {/* =======================================================
              OPERATING HOURS
          ======================================================= */}
          <h3 className="section-label">Operating Hours</h3>

          <div className="two-col">
            <div className="form-group">
              <label>Opening Time</label>
              <input
                type="time"
                value={form.openTime}
                onChange={(e) => setForm({ ...form, openTime: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label>Closing Time</label>
              <input
                type="time"
                value={form.closeTime}
                onChange={(e) => setForm({ ...form, closeTime: e.target.value })}
              />
            </div>
          </div>

          {/* =======================================================
              AVAILABILITY
          ======================================================= */}
          <div className="form-group">
            <label>Availability</label>
            <select
              value={form.isOnline}
              onChange={(e) => setForm({ ...form, isOnline: e.target.value === "true" })}
            >
              <option value="true">Online / Available</option>
              <option value="false">Offline / Not Available</option>
            </select>
          </div>

          {/* =======================================================
              PAYMENT METHOD
          ======================================================= */}
          <div className="form-group">
            <label>Preferred Payment Method</label>
            <select
              value={form.paymentMethod}
              onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}
            >
              <option value="mtn">MTN Mobile Money</option>
              <option value="airtel">Airtel Money</option>
              <option value="card">Card / Bank</option>
            </select>
          </div>

          {/* =======================================================
              BUSINESS LOGO
          ======================================================= */}
          <div className="form-group">
            <label>Business Logo</label>
            <input
              type="file"
              onChange={(e) => setForm({ ...form, logo: e.target.files[0] })}
            />
          </div>

          {/* =======================================================
              LICENSE / CERTIFICATION UPLOAD
          ======================================================= */}
          <div className="form-group">
            <label>Upload License / ID</label>
            <input
              type="file"
              onChange={(e) => setForm({ ...form, licenseFile: e.target.files[0] })}
            />
          </div>

          {/* =======================================================
              GPS LOCATION
          ======================================================= */}
          <div className="location-box">
            <div>
              <label>GPS Location</label>
              <p className="location-view">
                {form.lat && form.lng
                  ? `📍 ${form.lat.toFixed(6)}, ${form.lng.toFixed(6)}`
                  : "Location not set"}
              </p>
            </div>

            <button type="button" onClick={updateLocation} className="btn-outline small">
              Update GPS Location
            </button>
          </div>

          {/* =======================================================
              SAVE BUTTON
          ======================================================= */}
          <button className="btn-primary large" type="submit" disabled={saving}>
            {saving ? "Saving..." : "Save Changes"}
          </button>

        </form>
      </div>
    </div>
  );
}
