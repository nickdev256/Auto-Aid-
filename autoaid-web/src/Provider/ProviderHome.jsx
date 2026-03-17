// src/Provider/ProviderHome.jsx
import React, { useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

export default function ProviderHome() {
  const { user } = useAuth();
  const nav = useNavigate();

  useEffect(() => {
    // Don't run if user hasn't loaded yet
    if (!user) return;

    // If provider hasn't chosen service type
    if (!user.businessType) {
      nav("/provider/settings");
      return;
    }

    // Route based on provider business type
    switch (String(user.businessType).toLowerCase()) {
      case "garage":
        nav("/provider/garage");
        break;

      case "fuel":
        nav("/provider/fuel");
        break;

      case "towing":
        nav("/provider/towing");
        break;

      case "ambulance":
        nav("/provider/ambulance");
        break;

      default:
        nav("/provider/settings");
    }
  }, [user, nav]); // runs ONLY after render

  return <p>Loading provider dashboard...</p>;
}
