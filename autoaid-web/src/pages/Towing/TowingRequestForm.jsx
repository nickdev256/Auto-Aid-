import React, { useEffect, useState } from "react";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import "./TowingRequestForm.css";

export default function TowingRequestForm(){
  const { user } = useAuth();
  const nav = useNavigate();
  const [form, setForm] = useState({
    vehicleInfo: "",
    problemDescription: "",
    towType: "standard",
  });
  const [loc, setLoc] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(()=>{
    if (!navigator.geolocation) { alert("Enable location"); return; }
    navigator.geolocation.getCurrentPosition(
      p => setLoc({ lat: p.coords.latitude, lng: p.coords.longitude }),
      () => alert("Allow location"),
      { enableHighAccuracy:true }
    );
  },[]);

  const submit = async (e) => {
    e.preventDefault();
    if (!user) return nav("/login");
    if (!loc) return alert("Waiting for location...");
    setSubmitting(true);
    try {
      const payload = {
        userId: user.id,
        userName: user.name,
        userPhone: user.phone || "",
        vehicleInfo: form.vehicleInfo,
        problemDescription: form.problemDescription,
        towType: form.towType,
        lat: loc.lat,
        lng: loc.lng
      };
      const res = await axios.post("http://localhost:5001/api/towing/request", payload);
      nav(`/towing/status/${res.data.id}`);
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.message || "Request failed");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="towing-request-root">
      <div className="towing-card">
        <button className="back" onClick={() => nav(-1)}>⬅ Back</button>
        <h2>Request Towing</h2>
        <form className="towing-form" onSubmit={submit}>
          <label>Vehicle Info</label>
          <input value={form.vehicleInfo} onChange={e=>setForm({...form, vehicleInfo:e.target.value})} placeholder="e.g., Toyota Vitz, front axle damage" required />

          <label>Problem Description</label>
          <textarea value={form.problemDescription} onChange={e=>setForm({...form, problemDescription:e.target.value})} placeholder="Short description" rows={3} />

          <label>Tow Type</label>
          <select value={form.towType} onChange={e=>setForm({...form, towType:e.target.value})}>
            <option value="standard">Standard</option>
            <option value="flatbed">Flatbed</option>
          </select>

          <div className="location-brief">{loc ? <p>Your location: {loc.lat.toFixed(5)}, {loc.lng.toFixed(5)}</p> : <p>Fetching GPS...</p>}</div>

          <div className="form-actions">
            <button type="submit" className="primary" disabled={submitting}>{submitting? "Requesting..." : "Request Tow"}</button>
            <button type="button" className="ghost" onClick={()=>nav("/towing")}>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  );
}
