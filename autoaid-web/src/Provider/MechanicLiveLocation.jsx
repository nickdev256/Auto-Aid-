// src/Provider/MechanicLiveLocation.jsx
import React, { useEffect } from "react";
import axios from "axios";
import { useAuth } from "../context/AuthContext";

export default function MechanicLiveLocation() {
  const { user } = useAuth(); // mechanic logged in

  useEffect(() => {
    if (!user || user.role !== "provider") return;

    if (!navigator.geolocation) {
      alert("Enable GPS to share location");
      return;
    }

    const sendLocation = () => {
      navigator.geolocation.getCurrentPosition(
        async (pos) => {
          const payload = {
            providerId: user.id,
            lat: pos.coords.latitude,
            lng: pos.coords.longitude,
          };

          try {
            await axios.patch("http://localhost:5001/api/provider/update-location", payload);
            // console.log("GPS sent:", payload);
          } catch (err) {
            console.error("Location update failed:", err);
          }
        },
        (err) => console.error("GPS error:", err),
        { enableHighAccuracy: true }
      );
    };

    // Send location every 3 seconds
    const interval = setInterval(sendLocation, 3000);
    sendLocation(); // send immediately

    return () => clearInterval(interval);
  }, [user]);

  return null; // invisible background component
}
