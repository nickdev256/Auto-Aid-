// src/Provider/ProviderHome.jsx
import React, { useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

export default function ProviderHome() {
  const { user } = useAuth();
  const nav = useNavigate();

  useEffect(() => {
    if (!user) return;

    console.log("PROVIDER USER:", user);
    console.log("BUSINESS TYPE:", user?.businessType);

    const type = String(user?.businessType || "").toLowerCase().trim();

    if (!type) {
      console.warn("No businessType found on provider account");
      nav("/provider/settings");
      return;
    }

    if (type === "garage") {
      nav("/provider/garage");
      return;
    }

    if (type === "fuel") {
      nav("/provider/fuel");
      return;
    }

    if (type === "towing") {
      nav("/provider/towing");
      return;
    }

    if (type === "ambulance") {
      nav("/provider/ambulance");
      return;
    }

    console.warn("Unknown businessType:", type);
    nav("/provider/settings");
  }, [user, nav]);

  return <p>Loading provider dashboard...</p>;
}