// src/Provider/ProviderSettings.jsx
import React, { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { updateProviderBusiness } from "../services/api";
import { useNavigate } from "react-router-dom";
import "./BusinessSettings.css";

export default function ProviderSettings() {
  const { user, setUser } = useAuth();
  const navigate = useNavigate();

  if (!user) return <p>You must be logged in as a provider.</p>;

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

  const goBackToDashboard = () => {
    const type = String(user?.businessType || form?.businessType || "")
      .toLowerCase()
      .trim();

    if (type === "garage") {
      navigate("/provider/garage");
      return;
    }
    if (type === "fuel") {
      navigate("/provider/fuel");
      return;
    }
    if (type === "towing") {
      navigate("/provider/towing");
      return;
    }
    if (type === "ambulance") {
      navigate("/provider/ambulance");
      return;
    }

    navigate("/provider/dashboard");
  };

  const updateLocation = () => {
    if (!navigator.geolocation) {
      alert("Your device does not support GPS.");
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setForm((prev) => ({
          ...prev,
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
        }));
        alert("📍 GPS Location Updated!");
      },
      () => {
        alert("Enable GPS permissions to update location.");
      },
      { enableHighAccuracy: true }
    );
  };

  const saveChanges = async (e) => {
    e.preventDefault();
    setSaving(true);

    const payload = { ...form };
    delete payload.logo;
    delete payload.licenseFile;

    try {
      const providerId = user.id || user._id;
      const res = await updateProviderBusiness(providerId, payload);

      // support either:
      // 1) backend returns { user: {...} }
      // 2) backend returns {...userFields}
      const returnedUser = res?.user && typeof res.user === "object" ? res.user : res;

      const updatedUser = {
        ...user,
        ...returnedUser,
        businessType:
          returnedUser?.businessType ??
          payload.businessType ??
          user.businessType ??
          "",
        services: Array.isArray(returnedUser?.services)
          ? returnedUser.services
          : payload.services,
        businessName:
          returnedUser?.businessName ??
          payload.businessName ??
          user.businessName ??
          "",
        address:
          returnedUser?.address ??
          payload.address ??
          user.address ??
          "",
        description:
          returnedUser?.description ??
          payload.description ??
          user.description ??
          "",
        isOnline:
          typeof returnedUser?.isOnline === "boolean"
            ? returnedUser.isOnline
            : payload.isOnline,
        phone:
          returnedUser?.phone ??
          payload.phone ??
          user.phone ??
          "",
      };

      setUser(updatedUser);
      localStorage.setItem("auth_user", JSON.stringify(updatedUser));
      localStorage.removeItem("autoaid_user");

      alert("Settings updated successfully! 🎉");
      goBackToDashboard();
    } catch (err) {
      alert("❌ Update failed: " + (err?.message || "Something went wrong"));
    } finally {
      setSaving(false);
    }
  };

  const toggleService = (service) => {
    const selected = form.services.includes(service)
      ? form.services.filter((s) => s !== service)
      : [...form.services, service];

    setForm((prev) => ({ ...prev, services: selected }));
  };

  return (
    <div className="provider-settings">
      <button className="back-btn" onClick={goBackToDashboard}>
        ← Back to Dashboard
      </button>

      <div className="settings-header">
        <h1>Provider Business Settings</h1>
        <p>Update your business details & service preferences.</p>

        <div className={`verify-status ${user.isVerified ? "verified" : "pending"}`}>
          {user.isVerified ? "✔ Verified Provider" : "⏳ Pending Verification"}
        </div>
      </div>

      <div className="settings-card">
        <form className="settings-form" onSubmit={saveChanges}>
          <div className="two-col">
            <div className="form-group">
              <label>Business Name</label>
              <input
                value={form.businessName}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, businessName: e.target.value }))
                }
                placeholder="Enter your business name"
              />
            </div>

            <div className="form-group">
              <label>Phone Number</label>
              <input
                value={form.phone}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, phone: e.target.value }))
                }
                placeholder="+256 700 000000"
              />
            </div>
          </div>

          <div className="two-col">
            <div className="form-group">
              <label>Address</label>
              <input
                value={form.address}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, address: e.target.value }))
                }
                placeholder="Business address"
              />
            </div>

            <div className="form-group">
              <label>Primary Business Type</label>
              <select
                value={form.businessType}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, businessType: e.target.value }))
                }
              >
                <option value="">Select service type</option>
                <option value="garage">Garage / Mechanic</option>
                <option value="fuel">Fuel Delivery</option>
                <option value="towing">Towing</option>
                <option value="ambulance">Ambulance</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label>Description</label>
            <textarea
              rows={3}
              value={form.description}
              onChange={(e) =>
                setForm((prev) => ({ ...prev, description: e.target.value }))
              }
              placeholder="Describe your service"
            />
          </div>

          <h3 className="section-label">Services</h3>
          <div className="two-col">
            {["garage", "fuel", "towing", "ambulance"].map((service) => (
              <label
                key={service}
                className="form-group"
                style={{ display: "flex", gap: 8, alignItems: "center" }}
              >
                <input
                  type="checkbox"
                  checked={form.services.includes(service)}
                  onChange={() => toggleService(service)}
                />
                <span style={{ textTransform: "capitalize" }}>{service}</span>
              </label>
            ))}
          </div>

          <h3 className="section-label">Pricing</h3>

          <div className="two-col">
            <div className="form-group">
              <label>Base Price (UGX)</label>
              <input
                type="number"
                value={form.basePrice}
                onChange={(e) =>
                  setForm((prev) => ({
                    ...prev,
                    basePrice: Number(e.target.value),
                  }))
                }
              />
            </div>

            <div className="form-group">
              <label>Price Per KM (UGX)</label>
              <input
                type="number"
                value={form.pricePerKm}
                onChange={(e) =>
                  setForm((prev) => ({
                    ...prev,
                    pricePerKm: Number(e.target.value),
                  }))
                }
              />
            </div>
          </div>

          <h3 className="section-label">Operating Hours</h3>

          <div className="two-col">
            <div className="form-group">
              <label>Opening Time</label>
              <input
                type="time"
                value={form.openTime}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, openTime: e.target.value }))
                }
              />
            </div>

            <div className="form-group">
              <label>Closing Time</label>
              <input
                type="time"
                value={form.closeTime}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, closeTime: e.target.value }))
                }
              />
            </div>
          </div>

          <div className="form-group">
            <label>Availability</label>
            <select
              value={String(form.isOnline)}
              onChange={(e) =>
                setForm((prev) => ({
                  ...prev,
                  isOnline: e.target.value === "true",
                }))
              }
            >
              <option value="true">Online / Available</option>
              <option value="false">Offline / Not Available</option>
            </select>
          </div>

          <div className="form-group">
            <label>Preferred Payment Method</label>
            <select
              value={form.paymentMethod}
              onChange={(e) =>
                setForm((prev) => ({ ...prev, paymentMethod: e.target.value }))
              }
            >
              <option value="mtn">MTN Mobile Money</option>
              <option value="airtel">Airtel Money</option>
              <option value="card">Card / Bank</option>
            </select>
          </div>

          <div className="form-group">
            <label>Business Logo</label>
            <input
              type="file"
              onChange={(e) =>
                setForm((prev) => ({
                  ...prev,
                  logo: e.target.files?.[0] || null,
                }))
              }
            />
          </div>

          <div className="form-group">
            <label>Upload License / ID</label>
            <input
              type="file"
              onChange={(e) =>
                setForm((prev) => ({
                  ...prev,
                  licenseFile: e.target.files?.[0] || null,
                }))
              }
            />
          </div>

          <div className="location-box">
            <div>
              <label>GPS Location</label>
              <p className="location-view">
                {form.lat && form.lng
                  ? `📍 ${Number(form.lat).toFixed(6)}, ${Number(form.lng).toFixed(6)}`
                  : "Location not set"}
              </p>
            </div>

            <button
              type="button"
              onClick={updateLocation}
              className="btn-outline small"
            >
              Update GPS Location
            </button>
          </div>

          <button className="btn-primary large" type="submit" disabled={saving}>
            {saving ? "Saving..." : "Save Changes"}
          </button>
        </form>
      </div>
    </div>
  );
}