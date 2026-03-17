// src/Provider/ProviderLiveLocation.jsx
import React, { useEffect } from "react";
import axios from "axios";
import { useAuth } from "../context/AuthContext";

export default function ProviderLiveLocation() {
  const { user } = useAuth();

  useEffect(() => {
    if (!user || user.role !== "provider") return;
    if (!navigator.geolocation) {
      console.warn("Geolocation not supported");
      return;
    }

    const sendLoc = () => {
      navigator.geolocation.getCurrentPosition(async (pos) => {
        try {
          await axios.patch("http://localhost:5001/api/ambulance/update-location", {
            providerId: user.id,
            lat: pos.coords.latitude,
            lng: pos.coords.longitude
          });
        } catch (err) {
          console.error("update location failed", err);
        }
      }, (err) => { console.warn("gps err", err); }, { enableHighAccuracy: true });
    };

    sendLoc();
    const interval = setInterval(sendLoc, 3000);
    return () => clearInterval(interval);
  }, [user]);

  return null; // invisible
}
