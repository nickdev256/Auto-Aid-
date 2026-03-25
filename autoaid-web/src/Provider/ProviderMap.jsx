// src/pages/Provider/ProviderMap.jsx

import React, { useEffect, useState, useRef } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import axios from "axios";
import {
  GoogleMap,
  Marker,
  DirectionsRenderer,
  useJsApiLoader,
} from "@react-google-maps/api";

import "./ProviderMap.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

// ✅ FIXED – USE ENV VARIABLE CORRECTLY
const GOOGLE_API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

// FIX PERFORMANCE WARNING
const LIBRARIES = ["places"];

export default function ProviderMap() {
  const { id: requestId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const [request, setRequest] = useState(null);
  const [directions, setDirections] = useState(null);
  const mapRef = useRef(null);

  // For debugging – REMOVE later
  console.log("MAP KEY LOADED:", GOOGLE_API_KEY);

  // READ SERVICE TYPE FROM URL
  const query = new URLSearchParams(location.search);
  const forcedService = query.get("service");

  // LOAD GOOGLE MAPS SDK
  const { isLoaded } = useJsApiLoader({
    googleMapsApiKey: GOOGLE_API_KEY, // ✅ FIXED
    libraries: LIBRARIES, // ✅ FIXED
  });

  // --------------------------------------------
  // Fetch Request Based on service parameter
  // --------------------------------------------
  const loadRequest = async () => {
    try {
      let endpoint = null;

      if (forcedService) {
        endpoint = `${BASE}/api/${forcedService}/${requestId}`;
      }

      const endpoints = endpoint
        ? [endpoint]
        : [
            `${BASE}/api/garage/${requestId}`,
            `${BASE}/api/fuel/${requestId}`,
            `${BASE}/api/towing/${requestId}`,
            `${BASE}/api/ambulance/${requestId}`,
          ];

      for (const url of endpoints) {
        try {
          const res = await axios.get(url);
          if (res.data) {
            setRequest(res.data);

            if (res.data.providerLocation) {
              calcRoute(res.data.providerLocation, {
                lat: res.data.lat,
                lng: res.data.lng,
              });
            }
            return;
          }
        } catch {}
      }
    } catch (err) {
      console.error("❌ Could not load request:", err);
    }
  };

  const calcRoute = (origin, dest) => {
    if (!window.google) return;

    const service = new window.google.maps.DirectionsService();
    service.route(
      { origin, destination: dest, travelMode: "DRIVING" },
      (result, status) => {
        if (status === "OK") setDirections(result);
      }
    );
  };

  useEffect(() => {
    loadRequest();
    const iv = setInterval(loadRequest, 5000);
    return () => clearInterval(iv);
  }, [requestId, forcedService]);

  if (!isLoaded) return <p>Loading map...</p>;
  if (!request) return <p>Loading request...</p>;

  const center =
    request.providerLocation || { lat: request.lat, lng: request.lng };

  const titles = {
    garage: "Garage Navigation",
    towing: "Towing Navigation",
    fuel: "Fuel Delivery Navigation",
    ambulance: "Ambulance Navigation",
  };

  return (
    <div className="provider-map-page">
      <button onClick={() => navigate(-1)} className="back-btn">
        ← Back
      </button>

      <h1>{titles[request.serviceType] || "Navigation"}</h1>

      <div className="map-wrapper">
        <GoogleMap
          center={center}
          zoom={13}
          mapContainerStyle={{ width: "100%", height: "500px" }}
          onLoad={(map) => (mapRef.current = map)}
        >
          {request.providerLocation && (
            <Marker position={request.providerLocation} label="P" />
          )}

          <Marker position={{ lat: request.lat, lng: request.lng }} label="C" />

          {directions && <DirectionsRenderer directions={directions} />}
        </GoogleMap>
      </div>

      <div className="route-summary">
        <p><strong>User:</strong> {request.userName}</p>

        {directions?.routes?.[0]?.legs?.[0] && (
          <>
            <p><strong>Distance:</strong> {directions.routes[0].legs[0].distance.text}</p>
            <p><strong>ETA:</strong> {directions.routes[0].legs[0].duration.text}</p>
          </>
        )}
      </div>
    </div>
  );
}
