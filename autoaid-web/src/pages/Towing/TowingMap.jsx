import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import "./TowingMap.css";

export default function TowingMap(){
  const { id } = useParams();
  const nav = useNavigate();
  const [req, setReq] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    try {
      const res = await axios.get(`http://localhost:5001/api/towing/${id}`);
      setReq(res.data);
    } catch (err) {
      console.error(err);
      setReq(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(()=>{
    load();
    const iv = setInterval(load, 4000);
    return ()=>clearInterval(iv);
  },[id]);

  if (loading) return <p>Loading map...</p>;
  if (!req) return <div className="towing-map-root"><div className="towing-card"><button className="back" onClick={()=>nav(-1)}>⬅ Back</button><p>Not found</p></div></div>;

  return (
    <div className="towing-map-root">
      <div className="towing-card">
        <button className="back" onClick={()=>nav(-1)}>⬅ Back</button>
        <h2>Track Tow — {req.assignedToName || "Not Assigned"}</h2>

        <div className="map-info">
          <p><strong>User:</strong> {req.userName}</p>
          <p><strong>Pickup:</strong> {req.lat.toFixed(5)}, {req.lng.toFixed(5)}</p>
          {req.providerLocation ? (
            <p><strong>Tow at:</strong> {req.providerLocation.lat.toFixed(5)}, {req.providerLocation.lng.toFixed(5)}</p>
          ) : <p>Tow vehicle not yet visible</p>}
        </div>

        <div className="map-placeholder">
          <p>🗺 Replace with Google Maps / Leaflet integration (use live providerLocation)</p>
        </div>

        <div className="actions">
          <button className="ghost" onClick={()=>nav("/towing")}>Back</button>
        </div>
      </div>
    </div>
  );
}
