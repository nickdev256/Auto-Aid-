// src/pages/Provider/ProviderSignup.jsx
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

import "./ProviderSignup.css";

export default function ProviderSignup() {
  const navigate = useNavigate();
  const { signup } = useAuth();

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    phone: "",
    password: "",
    businessName: "",
    businessType: "",
    subscription: "free",
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Update form fields
  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  // Submit provider signup
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      await signup({
        ...formData,
        role: "provider",
        status: "pending", // must be approved by admin
      });

      alert("Provider request submitted! Waiting for admin approval.");
      navigate("/login");
    } catch (err) {
      console.error(err);
      setError(err?.message || "Failed to signup. Try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="provider-signup-container">
      <h2>Provider Signup</h2>
      <form onSubmit={handleSubmit} className="provider-signup-form">
        <input
          type="text"
          name="name"
          placeholder="Full Name"
          value={formData.name}
          onChange={handleChange}
          required
        />
        <input
          type="email"
          name="email"
          placeholder="Email"
          value={formData.email}
          onChange={handleChange}
          required
        />
        <input
          type="text"
          name="phone"
          placeholder="Phone"
          value={formData.phone}
          onChange={handleChange}
          required
        />
        <input
          type="password"
          name="password"
          placeholder="Password"
          value={formData.password}
          onChange={handleChange}
          required
        />
        <input
          type="text"
          name="businessName"
          placeholder="Business Name"
          value={formData.businessName}
          onChange={handleChange}
          required
        />

        <label>Type of Service</label>
        <select
          name="businessType"
          value={formData.businessType}
          onChange={handleChange}
          required
        >
          <option value="">Select Service</option>
          <option value="fuel">Fuel</option>
          <option value="garage">Garage/Mechanic</option>
          <option value="towing">Towing</option>
          <option value="ambulance">Ambulance</option>
        </select>

        <label>Subscription Plan</label>
        <select
          name="subscription"
          value={formData.subscription}
          onChange={handleChange}
        >
          <option value="free">Free</option>
          <option value="premium">Premium</option>
        </select>

        <button type="submit" disabled={loading}>
          {loading ? "Submitting..." : "Signup"}
        </button>

        {error && <p className="error">{error}</p>}
        <p className="note">
          Your account will be reviewed by the admin. You will receive approval to access the dashboard.
        </p>
      </form>
    </div>
  );
}
