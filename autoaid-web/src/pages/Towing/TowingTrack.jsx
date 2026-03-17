import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import { MapContainer, TileLayer, Marker, Polyline, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import "./TowingTrack.css";

export default function TowingTrack() {
  const { id } = useParams(); // towing request ID
  const navigate = useNavigate();

  const [request, setRequest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [eta, setEta] = useState(null);

  // Load latest data
  const fetchRequest = async () => {
    try {
      const res = await axios.get(`http://localhost:5001/api/towing/${id}`);
      setRequest(res.data);

      if (
        res.data?.providerLocation &&
        typeof res.data.lat === "number" &&
        typeof res.data.lng === "number"
      ) {
        calcETA(
          res.data.providerLocation.lat,
          res.data.providerLocation.lng,
          res.data.lat,
          res.data.lng
        );
      } else {
        setEta(null);
      }
    } catch (err) {
      console.error("Fetch towing track failed", err);
    } finally {
      setLoading(false);
    }
  };

  // Simple ETA calculation
  const calcETA = (pLat, pLng, uLat, uLng) => {
    const R = 6371;
    const toRad = (d) => (d * Math.PI) / 180;

    const dLat = toRad(uLat - pLat);
    const dLng = toRad(uLng - pLng);

    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(toRad(pLat)) * Math.cos(toRad(uLat)) * Math.sin(dLng / 2) ** 2;

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distanceKm = R * c;
    const speedKmh = 35; // approx speed

    const minutes = Math.max(1, Math.round((distanceKm / speedKmh) * 60));
    setEta({ distance: distanceKm.toFixed(2), minutes });
  };

  useEffect(() => {
    fetchRequest();
    const interval = setInterval(fetchRequest, 5000);
    return () => clearInterval(interval);
  }, []);

  if (loading) return <p>Loading tracking...</p>;
  if (!request) return <p>No tracking info available</p>;

  return (
    <div className="track-container">
      <button className="back-btn" onClick={() => navigate(-1)}>
        ← Back
      </button>

      <h2>Tracking Driver</h2>
      <p className="muted">Request ID: {id}</p>

      {eta && (
        <div className="eta-box">
          <p><strong>Distance:</strong> {eta.distance} km</p>
          <p><strong>ETA:</strong> {eta.minutes} min</p>
        </div>
      )}

      <div className="map-wrap">
        <MapContainer
          center={[request.lat, request.lng]}
          zoom={13}
          scrollWheelZoom={true}
          style={{
            height: "400px",
            width: "100%",
            borderRadius: "12px",
            marginTop: "10px"
          }}
        >
          <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

          {/* User position */}
          <Marker position={[request.lat, request.lng]}>
            <Popup>Your Location</Popup>
          </Marker>

          {/* Driver position */}
          {request.providerLocation && (
            <Marker position={[request.providerLocation.lat, request.providerLocation.lng]}>
              <Popup>Driver</Popup>
            </Marker>
          )}

          {/* Line between them */}
          {request.providerLocation && (
            <Polyline
              positions={[
                [request.lat, request.lng],
                [request.providerLocation.lat, request.providerLocation.lng],
              ]}
              color="blue"
            />
          )}
        </MapContainer>
      </div>
    </div>
  );
}
