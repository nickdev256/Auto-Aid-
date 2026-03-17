// src/pages/Garage/TrackRequest.jsx
// UNIVERSAL TRACKING FOR ALL SERVICES

import React, { useEffect, useState, useRef } from "react";
import { useParams, useLocation, useNavigate } from "react-router-dom";
import axios from "axios";
import {
  GoogleMap,
  Marker,
  DirectionsRenderer,
  useJsApiLoader,
} from "@react-google-maps/api";

import "./TrackRequest.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";
const GOOGLE_API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

export default function TrackRequest() {
  const { id: requestId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const [request, setRequest] = useState(null);
  const [directions, setDirections] = useState(null);

  const mapRef = useRef(null);

  // Detect service from ?service=garage
  const query = new URLSearchParams(location.search);
  const forcedService = query.get("service");

  const { isLoaded } = useJsApiLoader({
    googleMapsApiKey: GOOGLE_API_KEY,
    libraries: ["places"],
  });

  // --------------------------------------------
  // UNIVERSAL FETCHER FOR ANY SERVICE
  // --------------------------------------------
  const loadRequest = async () => {
    try {
      let endpoint = null;

      // If the page explicitly tells us the service
      if (forcedService) {
        endpoint = `${BASE}/api/${forcedService}/${requestId}`;
      }

      // Otherwise try all service routes
      const endpoints = endpoint
        ? [endpoint]
        : [
            `${BASE}/api/garage/${requestId}`,
            `${BASE}/api/fuel/${requestId}`,
            `${BASE}/api/towing/${requestId}`,
            `${BASE}/api/ambulance/${requestId}`,
          ];

      // Try each API until one returns data
      for (const url of endpoints) {
        try {
          const res = await axios.get(url);

          if (res.data) {
            setRequest(res.data);

            // If provider is moving → draw directions
            if (res.data.providerLocation) {
              calcRoute(res.data.providerLocation, {
                lat: res.data.lat,
                lng: res.data.lng,
              });
            }

            return;
          }
        } catch (err) {
          // Continue to next
        }
      }
    } catch (err) {
      console.error("❌ Cannot load tracking info", err);
    }
  };

  // --------------------------------------------
  // GOOGLE ROUTE BUILDER
  // --------------------------------------------
  const calcRoute = (origin, dest) => {
    if (!window.google) return;

    const service = new window.google.maps.DirectionsService();
    service.route(
      {
        origin,
        destination: dest,
        travelMode: "DRIVING",
      },
      (result, status) => {
        if (status === "OK") setDirections(result);
      }
    );
  };

  useEffect(() => {
    loadRequest();

    const iv = setInterval(loadRequest, 5000); // refresh every 5s
    return () => clearInterval(iv);
  }, [requestId, forcedService]);

  if (!isLoaded) return <p>Loading map…</p>;
  if (!request) return <p>Loading tracking details…</p>;

  const center =
    request.providerLocation || { lat: request.lat, lng: request.lng };

  const titles = {
    garage: "Tracking Mechanic",
    towing: "Tracking Tow Truck",
    fuel: "Tracking Fuel Provider",
    ambulance: "Tracking Ambulance",
  };

  return (
    <div className="track-page">
      <button onClick={() => navigate(-1)} className="back-btn">
        ← Back
      </button>

      <h1>{titles[request.serviceType] || "Tracking Service Provider"}</h1>

      <div className="map-wrapper">
        <GoogleMap
          center={center}
          zoom={14}
          mapContainerStyle={{ width: "100%", height: "500px" }}
          onLoad={(m) => (mapRef.current = m)}
        >
          {/* Provider Moving Marker */}
          {request.providerLocation && (
            <Marker position={request.providerLocation} label="P" />
          )}

          {/* Customer Marker */}
          <Marker position={{ lat: request.lat, lng: request.lng }} label="You" />

          {directions && <DirectionsRenderer directions={directions} />}
        </GoogleMap>
      </div>

      <div className="route-summary">
        <p>
          <strong>Provider:</strong> {request.assignedToName || "Not assigned yet"}
        </p>

        {directions?.routes?.[0]?.legs?.[0] && (
          <>
            <p>
              <strong>Distance:</strong>{" "}
              {directions.routes[0].legs[0].distance.text}
            </p>
            <p>
              <strong>Estimated Arrival:</strong>{" "}
              {directions.routes[0].legs[0].duration.text}
            </p>
          </>
        )}

        {!request.providerLocation && (
          <p className="waiting-text">Waiting for provider to start moving…</p>
        )}
      </div>
    </div>
  );
}
