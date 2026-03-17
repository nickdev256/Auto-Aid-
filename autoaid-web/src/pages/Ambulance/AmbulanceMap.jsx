// src/pages/Ambulance/AmbulanceMap.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";
import { useParams } from "react-router-dom";

export default function AmbulanceMap() {
  const { id } = useParams();
  const [data, setData] = useState(null);

  const load = async () => {
    try {
      const res = await axios.get(`http://localhost:5001/api/ambulance/track/${id}`);
      setData(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    load();
    const iv = setInterval(load, 3000);
    return () => clearInterval(iv);
  }, []);

  if (!data) return <p>Loading map...</p>;

  return (
    <div style={{ padding: "20px" }}>
      <h2>Ambulance Tracking</h2>

      <div
        style={{
          height: "400px",
          background: "#eef3ff",
          borderRadius: "10px",
          textAlign: "center",
          paddingTop: "150px",
          fontWeight: 700
        }}
      >
        User: {data.userLocation.lat}, {data.userLocation.lng} <br />
        Ambulance: {data.providerLocation?.lat}, {data.providerLocation?.lng} <br />
        Status: {data.status}
      </div>
    </div>
  );
}
