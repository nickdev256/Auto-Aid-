import React, { useEffect, useState } from "react";
import axios from "axios";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";
import { useNavigate } from "react-router-dom";
import "./NearbyGarages.css";

const garageIcon = new L.Icon({ iconUrl: "/images/garage-marker.png", iconSize: [40,40] });

const calcDistance = (lat1, lon1, lat2, lon2) => {
  const R = 6371;
  const toRad = d => d * Math.PI / 180;
  const dLat = toRad(lat2 - lat1), dLon = toRad(lon2 - lon1);
  const a = Math.sin(dLat/2)**2 + Math.cos(toRad(lat1))*Math.cos(toRad(lat2))*Math.sin(dLon/2)**2;
  return R * (2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a)));
};

export default function NearbyGarages() {
  const [userLocation, setUserLocation] = useState(null);
  const [garages, setGarages] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    navigator.geolocation.getCurrentPosition(pos => setUserLocation({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      (err) => { alert("Enable location"); setLoading(false); }, { enableHighAccuracy: true });
  }, []);

  useEffect(() => {
    if (!userLocation) return;
    const load = async () => {
      try {
        const res = await axios.post(`${import.meta.env.VITE_API_URL || "http://localhost:5001"}/api/garages/nearby`, { lat: userLocation.lat, lng: userLocation.lng });
        const list = res.data.garages || [];
        const processed = list.map(g => {
          const glat = Number(g.lat) || 0; const glng = Number(g.lng) || 0;
          const distanceKm = calcDistance(userLocation.lat, userLocation.lng, glat, glng).toFixed(2);
          return { ...g, _lat: glat, _lng: glng, distanceKm };
        }).sort((a,b)=> parseFloat(a.distanceKm)-parseFloat(b.distanceKm));
        setGarages(processed);
      } catch (err) {
        alert("Failed to load garages");
      } finally { setLoading(false); }
    };
    load();
  }, [userLocation]);

  if (loading) return <div className="nearby-loading">Loading nearby garages…</div>;
  if (!userLocation) return <div className="nearby-empty">Enable location to see nearby garages</div>;

  return (
    <div className="nearby-container">
      <h1>Nearby Garages</h1>
      <div className="nearby-grid">
        <div className="map-column">
          <MapContainer center={[userLocation.lat, userLocation.lng]} zoom={13} style={{ height: 440 }}>
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            <Marker position={[userLocation.lat, userLocation.lng]}>
              <Popup>You are here</Popup>
            </Marker>
            {garages.map(g => (
              <Marker key={g._id || g.id} position={[g._lat, g._lng]} icon={garageIcon}>
                <Popup>
                  <strong>{g.businessName}</strong><br />
                  {g.distanceKm} km away<br />
                  <button onClick={() => navigate(`/garage/request?garageId=${g._id || g.id}`)}>Request Service</button>
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        </div>

        <div className="list-column">
          {garages.map(g => (
            <div className="garage-card" key={g._id || g.id}>
              <h3>{g.businessName}</h3>
              <p><strong>Distance:</strong> {g.distanceKm} km</p>
              <p><strong>Phone:</strong> {g.phone || "Not provided"}</p>
              <p><strong>Address:</strong> {g.address || "No address"}</p>
              <div className="card-actions">
                <button onClick={() => navigate(`/garage/request?garageId=${g._id || g.id}`)} className="request-btn">Request</button>
                <button onClick={() => navigate(`/garage/${g._id || g.id}`)} className="profile-btn">Profile</button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
