import React, { useEffect, useRef, useState } from "react";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import io from "socket.io-client";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "./ActiveRequest.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";
const SOCKET_URL = import.meta.env.VITE_SOCKET_URL || BASE;

export default function ActiveRequest() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [request, setRequest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [timeline, setTimeline] = useState([]);
  const socketRef = useRef(null);
  const mapRef = useRef(null);
  const mechMarkerRef = useRef(null);
  const userMarkerRef = useRef(null);

  useEffect(() => {
    if (!user) return navigate("/login");

    const loadLatest = async () => {
      try {
        const res = await axios.get(`${BASE}/api/garage/latest/${user._id}`);
        setRequest(res.data);
        setTimeline(res.data?.history || []);
      } catch {
        setRequest(null);
      } finally { setLoading(false); }
    };

    loadLatest();
    const iv = setInterval(loadLatest, 8000);
    return ()=>clearInterval(iv);
  }, [user]);

  useEffect(() => {
    if (!request) return;
    socketRef.current = io(SOCKET_URL, { transports: ["websocket", "polling"] });
    const socket = socketRef.current;
    socket.on("connect", ()=>{ if (request.requestId) socket.emit("join_request", request.requestId); });
    socket.on("request_update", updated => { setRequest(updated); setTimeline(updated.history || []); });
    socket.on("location_update", loc => {
      if (!mapRef.current) return;
      updateMechanicMarker(loc.lat, loc.lng);
    });
    return ()=> { socket.emit("leave_request", request.requestId); socket.disconnect(); };
    // eslint-disable-next-line
  }, [request?.requestId]);

  useEffect(() => {
    if (!request || !request.lat || !request.lng) return;
    if (!mapRef.current) {
      const map = L.map("liveMap").setView([request.lat, request.lng], 13);
      mapRef.current = map;
      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png").addTo(map);
      userMarkerRef.current = L.marker([request.lat, request.lng], { title: "You" }).addTo(map);
      if (request.liveLocation?.lat) {
        mechMarkerRef.current = L.marker([request.liveLocation.lat, request.liveLocation.lng]).addTo(map);
      }
    } else {
      userMarkerRef.current.setLatLng([request.lat, request.lng]);
    }
    if (request.liveLocation?.lat) updateMechanicMarker(request.liveLocation.lat, request.liveLocation.lng);
    // eslint-disable-next-line
  }, [request]);

  const updateMechanicMarker = (lat, lng) => {
    if (!mapRef.current) return;
    if (!mechMarkerRef.current) mechMarkerRef.current = L.marker([lat, lng]).addTo(mapRef.current);
    else mechMarkerRef.current.setLatLng([lat, lng]);

    const bounds = L.latLngBounds([[request.lat, request.lng],[lat,lng]]);
    mapRef.current.fitBounds(bounds.pad(0.4));
  };

  if (loading) return <div className="ar-loading">Loading…</div>;
  if (!request) return (
    <div className="empty">
      <h2>No Active Request</h2>
      <button onClick={() => navigate("/garage/request")} className="primary">Create Request</button>
    </div>
  );

  return (
    <div className="active-request-page">
      <button className="back-btn" onClick={() => navigate(-1)}>← Back</button>
      <h1>Live Request</h1>

      <div className="status-top">
        <div className="status-pill">{request.status?.toUpperCase()}</div>
        <div className="small-muted">Request ID: {request.requestId}</div>
      </div>

      <div className="card-grid">
        <div className="left-col">
          <div className="req-info">
            <h3>Issue</h3><p>{request.issue}</p>
            <h3>Vehicle</h3><p>{request.vehicleInfo}</p>
            <h3>Address</h3><p>{request.address || "—"}</p>
            <div className="actions">
              {request.status === "pending" && <button className="btn-cancel" onClick={async ()=>{ if (!confirm("Cancel?")) return; await axios.post(`${BASE}/api/garage/cancel/${request.requestId}`); alert("Cancelled"); navigate("/garage"); }}>Cancel Request</button>}
              <button className="btn-chat" onClick={() => navigate(`/garage/chat/${request.requestId}`)}>Open Chat</button>
              {request.status === "on_way" && <button className="btn-track" onClick={() => navigate(`/garage/track/${request.requestId}`)}>Track Mechanic</button>}
            </div>
          </div>

          <div className="timeline-wrap">
            <h4>Timeline</h4>
            <ul className="timeline">
              {timeline.map((t, i) => (
                <li key={i}>
                  <div className={`dot ${t.status}`}></div>
                  <div className="tn-content">
                    <div className="tn-status">{t.status.toUpperCase()}</div>
                    <div className="tn-note">{t.note}</div>
                    <div className="tn-time">{new Date(t.at).toLocaleString()}</div>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="right-col">
          <div id="liveMap" className="live-map" />
          <div className="mech-card">
            <div className="mech-avatar">{request.assignedToName ? request.assignedToName[0].toUpperCase() : "—"}</div>
            <div className="mech-info">
              <div className="mech-name">{request.assignedToName || "Not assigned yet"}</div>
              <div className="mech-meta">{request.assignedVehicle || ""}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
