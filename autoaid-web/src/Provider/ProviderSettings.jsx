// src/pages/Provider/ProviderSettings.jsx
import React, { useEffect, useState } from "react";
import { updateProviderProfile } from "../../services/api";
import { useAuth } from "../../context/AuthContext";

export default function ProviderSettings() {
  const { user, loginUser } = useAuth();
  const [form, setForm] = useState({
    businessName: "",
    phone: "",
    servicesOffered: "",
    paymentMethod: "mtn",
    lat: "",
    lng: "",
    address: ""
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) {
      setForm({
        businessName: user.businessName || "",
        phone: user.phone || "",
        servicesOffered: (user.servicesOffered || []).join(", "),
        paymentMethod: user.paymentMethod || "mtn",
        lat: user.lat || "",
        lng: user.lng || "",
        address: user.address || ""
      });
    }
  }, [user]);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!user) return alert("Login required");

    setLoading(true);
    try {
      const payload = {
        id: user.id,
        businessName: form.businessName,
        phone: form.phone,
        servicesOffered: form.servicesOffered.split(",").map(s => s.trim()).filter(Boolean),
        paymentMethod: form.paymentMethod,
        lat: form.lat ? parseFloat(form.lat) : undefined,
        lng: form.lng ? parseFloat(form.lng) : undefined,
        address: form.address
      };
      const updated = await updateProviderProfile(payload);
      alert("Profile updated");
      // optionally update context user
      loginUser(updated.provider, user.token || null);
    } catch (err) {
      alert(err.message || "Update failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: 16 }}>
      <h2>Business Settings</h2>
      <form onSubmit={handleSubmit} style={{ maxWidth: 720 }}>
        <input name="businessName" value={form.businessName} onChange={handleChange} placeholder="Business Name" />
        <input name="phone" value={form.phone} onChange={handleChange} placeholder="Phone" />
        <input name="servicesOffered" value={form.servicesOffered} onChange={handleChange} placeholder="Services (comma separated)" />
        <select name="paymentMethod" value={form.paymentMethod} onChange={handleChange}>
          <option value="mtn">MTN</option>
          <option value="airtel">Airtel</option>
          <option value="card">Card</option>
        </select>
        <input name="lat" value={form.lat} onChange={handleChange} placeholder="Latitude" />
        <input name="lng" value={form.lng} onChange={handleChange} placeholder="Longitude" />
        <input name="address" value={form.address} onChange={handleChange} placeholder="Address" />
        <button type="submit" disabled={loading}>{loading ? "Saving..." : "Save Settings"}</button>
      </form>
    </div>
  );
}
