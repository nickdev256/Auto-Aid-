import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import axios from "axios";
import { MapContainer, TileLayer, Marker, Polyline, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import "./TowingActive.css";


export default function TowingActive() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [request, setRequest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [eta, setEta] = useState(null);
  const [error, setError] = useState(null);

  const fetchLatest = async () => {
    if (!user) return;
    try {
      const res = await axios.get(`http://localhost:5001/api/towing/latest/${user.id}`);
      setRequest(res.data);
      setError(null);

      
      if (res.data?.providerLocation && typeof res.data.lat === "number" && typeof res.data.lng === "number") {
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
    e
      setRequest(null);
     
      if (err.response && err.response.status === 404) {
        setError(null);
      } else {
        setError("Failed to fetch active request. Check your backend.");
      }
    } finally {
      setLoading(false);
    }
  };

  
  const calcETA = (mLat, mLng, uLat, uLng) => {
    const R = 6371;
    const toRad = (d) => (d * Math.PI) / 180;
    const dLat = toRad(uLat - mLat);
    const dLng = toRad(uLng - mLng);
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(toRad(mLat)) * Math.cos(toRad(uLat)) * Math.sin(dLng / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distanceKm = R * c;
    const speedKmh = 35;
    const minutes = Math.max(1, Math.round((distanceKm / speedKmh) * 60));
    setEta({ distance: distanceKm.toFixed(2), minutes });
  };

  useEffect(() => {
    fetchLatest();
    const interval = setInterval(fetchLatest, 5000);
    return () => clearInterval(interval);
  
  }, [user]);

  const cancelRequest = async () => {
    if (!request) return;
    if (!window.confirm("Cancel this towing request?")) return;
    try {
      await axios.patch(`http://localhost:5001/api/towing/${request.id}/status`, { status: "cancelled" });
      setRequest(null);
      alert("Request cancelled.");
      navigate("/dashboard");
    } catch (err) {
      console.error("Cancel failed", err);
      alert(err.response?.data?.message || "Failed to cancel request.");
    }
  };

  if (loading) {
    return (
      <div className="towing-root">
        <div className="towing-card towing-loading">
          <div className="skeleton-title" />
          <p className="muted">Loading your active towing request…</p>
        </div>
      </div>
    );
  }

  if (!request) {
    return (
      <div className="towing-root">
        <div className="towing-card empty">
          <button className="back" onClick={() => navigate(-1)}>← Back</button>
          <h2>No Active Towing Request</h2>
          <p className="muted">You don't have a current towing job. Request one from the Towing page.</p>
          <div className="cta-row">
            <button className="btn primary" onClick={() => navigate("/towing/request")}>Request Tow</button>
            <button className="btn ghost" onClick={() => navigate("/towing")}>Browse Towing Services</button>
          </div>
          {error && <p className="error">{error}</p>}
        </div>
      </div>
    );
  }

 
  return (
    <div className="towing-root">
      <div className="towing-card">
        <button className="back" onClick={() => navigate(-1)}>← Back</button>

        <header className="header">
          <div>
            <h2>Active Tow</h2>
            <p className="muted">Request ID: <span className="muted-strong">{request.id}</span></p>
          </div>

          <div className="status-block">
            <div className={`status-pill ${request.status?.replace(/\s+/g, "").toLowerCase()}`}>
              {request.status || "pending"}
            </div>
          </div>
        </header>

        <section className="details">
          <div className="left">
            <p><strong>Service:</strong> {request.serviceType || "Towing"}</p>
            <p><strong>Issue:</strong> {request.issue || request.vehicleInfo || "Not specified"}</p>
            <p><strong>Pickup:</strong> {request.address}</p>
          </div>

          <div className="right">
            <p><strong>User:</strong> {request.userName}</p>
            <p><strong>Phone:</strong> {request.userPhone || "N/A"}</p>
            <p><strong>Requested:</strong> {new Date(request.createdAt).toLocaleString()}</p>
          </div>
        </section>

        {request.assignedTo ? (
          <>
            <div className="provider-card">
              <div>
                <h3>Driver Assigned</h3>
                <p className="muted">ID: {request.assignedTo}</p>
                {request.providerName && <p className="muted">Name: {request.providerName}</p>}
              </div>

              <div className="eta-block">
                {eta ? (
                  <>
                    <p className="muted">Distance</p>
                    <p className="big">{eta.distance} km</p>
                    <p className="muted">ETA</p>
                    <p className="big">{eta.minutes} min</p>
                  </>
                ) : <p className="muted">Calculating ETA…</p>}
              </div>
            </div>

            <div className="map-area">
              <MapContainer
                center={[request.lat || 0, request.lng || 0]}
                zoom={13}
                style={{ height: 320, borderRadius: 12 }}
                scrollWheelZoom={false}
              >
                <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                <Marker position={[request.lat || 0, request.lng || 0]}>
                  <Popup>Your location</Popup>
                </Marker>

                {request.providerLocation && (
                  <Marker position={[request.providerLocation.lat, request.providerLocation.lng]}>
                    <Popup>Driver</Popup>
                  </Marker>
                )}

                {request.providerLocation && (
                  <Polyline
                    positions={[
                      [request.lat, request.lng],
                      [request.providerLocation.lat, request.providerLocation.lng]
                    ]}
                  />
                )}
              </MapContainer>
            </div>

            <div className="actions-row">
              <button className="btn primary" onClick={() => navigate(`/towing/track/${request.id}`)}>Track Driver</button>
              <button className="btn secondary" onClick={() => navigate(`/towing/chat/${request.id}`)}>Chat</button>
              <button className="btn ghost danger" onClick={cancelRequest}>Cancel</button>
            </div>
          </>
        ) : (
          <div className="waiting-block">
            <p className="muted">⏳ Waiting for a driver to accept your request. Keep this page open — you'll be notified when a driver accepts.</p>
            <div className="actions-row">
              <button className="btn ghost" onClick={() => navigate("/towing/request")}>Modify Request</button>
              <button className="btn ghost danger" onClick={cancelRequest}>Cancel</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
