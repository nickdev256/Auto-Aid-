import React, { useEffect, useState } from "react";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import "./TowingHistory.css";

export default function TowingHistory(){
  const { user } = useAuth();
  const nav = useNavigate();
  const [list, setList] = useState([]);

  useEffect(()=>{
    if (!user) return;
    const load = async ()=> {
      try {
        const res = await axios.get(`http://localhost:5001/api/towing/history/${user.id}`);
        setList(res.data || []);
      } catch (err) {
        console.error("Load history", err);
      }
    };
    load();
  },[user]);

  return (
    <div className="towing-history-root">
      <div className="towing-card">
        <button className="back" onClick={()=>nav(-1)}>⬅ Back</button>
        <h2>History</h2>
        {list.length === 0 ? <div className="empty">No towing orders yet</div> :
          <div className="history-list">
            {list.map(h => (
              <div key={h.id} className="history-card">
                <div className="row">
                  <p className="title">{h.towType.toUpperCase()} • {h.vehicleInfo || "Vehicle"}</p>
                  <div className={`badge ${h.status.replace(/\s+/g,'')}`}>{h.status}</div>
                </div>
                <p className="muted">{new Date(h.createdAt).toLocaleString()}</p>
              </div>
            ))}
          </div>
        }
      </div>
    </div>
  );
}
