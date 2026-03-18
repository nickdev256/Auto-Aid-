// src/pages/Provider/ProviderMap.jsx

import React, { useEffect, useState, useRef, useCallback, useMemo } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import axios from "axios";
import {
  GoogleMap,
  Marker,
  DirectionsRenderer,
  useJsApiLoader,
} from "@react-google-maps/api";
console.log("ALL ENV:", import.meta.env);
import "./ProviderMap.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";
const GOOGLE_API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || "";
const LIBRARIES = ["places"];

export default function ProviderMap() {
  const { id: requestId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const mapRef = useRef(null);

  const [request, setRequest] = useState(null);
  const [directions, setDirections] = useState(null);
  const [loadingRequest, setLoadingRequest] = useState(true);
  const [requestError, setRequestError] = useState("");

  const query = new URLSearchParams(location.search);
  const forcedService = query.get("service");

  console.log("MAP KEY LOADED:", GOOGLE_API_KEY || "undefined");

  const { isLoaded, loadError } = useJsApiLoader({
    googleMapsApiKey: GOOGLE_API_KEY,
    libraries: LIBRARIES,
  });

  const titles = useMemo(
    () => ({
      garage: "Garage Navigation",
      towing: "Towing Navigation",
      fuel: "Fuel Delivery Navigation",
      ambulance: "Ambulance Navigation",
    }),
    []
  );

  const normalizeLatLng = (value) => {
    if (!value) return null;

    const lat = Number(value.lat);
    const lng = Number(value.lng);

    if (Number.isNaN(lat) || Number.isNaN(lng)) return null;

    return { lat, lng };
  };

  const calcRoute = useCallback((origin, destination) => {
    if (!window.google) return;
    if (!GOOGLE_API_KEY) {
      console.warn("Google Maps API key missing. Directions skipped.");
      setDirections(null);
      return;
    }
    if (!origin || !destination) {
      setDirections(null);
      return;
    }

    const directionsService = new window.google.maps.DirectionsService();

    directionsService.route(
      {
        origin,
        destination,
        travelMode: window.google.maps.TravelMode.DRIVING,
      },
      (result, status) => {
        if (status === "OK" && result) {
          setDirections(result);
        } else {
          console.warn("Directions request failed:", status);
          setDirections(null);
        }
      }
    );
  }, [GOOGLE_API_KEY]);

  const loadRequest = useCallback(async () => {
    try {
      setLoadingRequest(true);
      setRequestError("");

      const candidateEndpoints = [
        `${BASE}/api/requests/${requestId}`,
        `${BASE}/api/request/${requestId}`,
        `${BASE}/api/provider/requests/${requestId}`,
      ];

      let found = null;

      for (const url of candidateEndpoints) {
        try {
          const res = await axios.get(url);
          if (res?.data) {
            found = res.data;
            break;
          }
        } catch (_) {
          // try next endpoint
        }
      }

      if (!found) {
        throw new Error("Request not found");
      }

      setRequest(found);

      const providerPosition = normalizeLatLng(found.providerLocation);
      const userPosition =
        normalizeLatLng(found.userLocation) ||
        normalizeLatLng({
          lat: found.lat,
          lng: found.lng,
        });

      if (providerPosition && userPosition) {
        calcRoute(providerPosition, userPosition);
      } else {
        setDirections(null);
      }
    } catch (err) {
      console.error("Could not load request:", err);
      setRequest(null);
      setDirections(null);
      setRequestError(err?.message || "Failed to load request");
    } finally {
      setLoadingRequest(false);
    }
  }, [BASE, requestId, calcRoute]);

  useEffect(() => {
    loadRequest();
  }, [loadRequest]);

  if (!GOOGLE_API_KEY) {
    return (
      <div className="provider-map-page">
        <button onClick={() => navigate(-1)} className="back-btn">
          ← Back
        </button>

        <h1>{titles[(forcedService || "").toLowerCase()] || "Navigation"}</h1>

        <div className="route-summary">
          <p><strong>Google Maps API key is missing.</strong></p>
          <p>Add <code>VITE_GOOGLE_MAPS_API_KEY</code> to your frontend <code>.env</code> file, then restart Vite.</p>
        </div>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="provider-map-page">
        <button onClick={() => navigate(-1)} className="back-btn">
          ← Back
        </button>

        <h1>Navigation</h1>

        <div className="route-summary">
          <p><strong>Failed to load Google Maps.</strong></p>
          <p>Please check your API key and Google Cloud settings.</p>
        </div>
      </div>
    );
  }

  if (!isLoaded) {
    return (
      <div className="provider-map-page">
        <button onClick={() => navigate(-1)} className="back-btn">
          ← Back
        </button>
        <p>Loading map...</p>
      </div>
    );
  }

  if (loadingRequest) {
    return (
      <div className="provider-map-page">
        <button onClick={() => navigate(-1)} className="back-btn">
          ← Back
        </button>
        <p>Loading request...</p>
      </div>
    );
  }

  if (requestError) {
    return (
      <div className="provider-map-page">
        <button onClick={() => navigate(-1)} className="back-btn">
          ← Back
        </button>

        <h1>Navigation</h1>

        <div className="route-summary">
          <p><strong>Error:</strong> {requestError}</p>
          <button onClick={loadRequest} className="back-btn">Retry</button>
        </div>
      </div>
    );
  }

  if (!request) {
    return (
      <div className="provider-map-page">
        <button onClick={() => navigate(-1)} className="back-btn">
          ← Back
        </button>
        <p>No request found.</p>
      </div>
    );
  }

  const providerPosition = normalizeLatLng(request.providerLocation);
  const userPosition =
    normalizeLatLng(request.userLocation) ||
    normalizeLatLng({
      lat: request.lat,
      lng: request.lng,
    });

  const center = providerPosition || userPosition || { lat: 0, lng: 0 };

  const serviceKey = (
    request.service ||
    request.providerType ||
    request.serviceType ||
    forcedService ||
    ""
  )
    .toString()
    .trim()
    .toLowerCase();

  return (
    <div className="provider-map-page">
      <button onClick={() => navigate(-1)} className="back-btn">
        ← Back
      </button>

      <h1>{titles[serviceKey] || "Navigation"}</h1>

      <div className="map-wrapper">
        <GoogleMap
          center={center}
          zoom={13}
          mapContainerStyle={{ width: "100%", height: "500px" }}
          onLoad={(map) => {
            mapRef.current = map;
          }}
        >
          {providerPosition && <Marker position={providerPosition} label="P" />}
          {userPosition && <Marker position={userPosition} label="C" />}
          {directions && <DirectionsRenderer directions={directions} />}
        </GoogleMap>
      </div>

      <div className="route-summary">
        <p><strong>User:</strong> {request.userName || "Unknown"}</p>
        <p><strong>Service:</strong> {request.service || request.providerType || request.serviceType || "Unknown"}</p>

        {userPosition && (
          <p><strong>User Coordinates:</strong> {userPosition.lat}, {userPosition.lng}</p>
        )}

        {providerPosition && (
          <p><strong>Provider Coordinates:</strong> {providerPosition.lat}, {providerPosition.lng}</p>
        )}

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