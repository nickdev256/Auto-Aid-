// src/pages/Chat/UniversalChatList.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import "../../styles/UniversalChatList.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function UniversalChatList() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [convs, setConvs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    loadAll();
  }, [user]);

  const loadAll = async () => {
    setLoading(true);
    const providerId = user?._id || user?.id;

    let endpoints = [];

    if (user.role === "mechanic") {
      endpoints.push(`${BASE}/api/garage/byProvider/${providerId}`);
    }
    if (user.role === "fuel_provider") {
      endpoints.push(`${BASE}/api/fuel/byProvider/${providerId}`);
    }
    if (user.role === "towing_provider") {
      endpoints.push(`${BASE}/api/towing/byProvider/${providerId}`);
    }
    if (user.role === "ambulance_provider") {
      endpoints.push(`${BASE}/api/ambulance/byProvider/${providerId}`);
    }
    if (user.role === "admin") {
      endpoints = [
        `${BASE}/api/garage/byProvider/${providerId}`,
        `${BASE}/api/fuel/byProvider/${providerId}`,
        `${BASE}/api/towing/byProvider/${providerId}`,
        `${BASE}/api/ambulance/byProvider/${providerId}`,
      ];
    }

    try {
      const responses = await Promise.allSettled(
        endpoints.map(ep => axios.get(ep))
      );

      const allRequests = [];

      responses.forEach(res => {
        if (res.status === "fulfilled" && Array.isArray(res.value.data)) {
          allRequests.push(...res.value.data);
        }
      });

      const formatted = allRequests.map(req => ({
        requestId: req.requestId,
        serviceType: req.serviceType,
        userName: req.userName,
        status: req.status,
        lastMessage: req.chat?.length
          ? req.chat[req.chat.length - 1]
          : null,
      }));

      formatted.sort((a, b) => {
        const ta = a.lastMessage
          ? new Date(a.lastMessage.time)
          : 0;
        const tb = b.lastMessage
          ? new Date(b.lastMessage.time)
          : 0;

        return tb - ta;
      });

      setConvs(formatted);
    } catch (err) {
      console.error("LOAD ERROR:", err);
    }

    setLoading(false);
  };

  if (!user) return <p>Please log in.</p>;

  return (
    <div className="conv-root">
      <h2>Chats</h2>

      {loading ? (
        <p>Loading…</p>
      ) : convs.length === 0 ? (
        <p>No conversations yet</p>
      ) : (
        <div className="conv-list">
          {convs.map((c, i) => (
            <div
              key={i}
              className="conv-card"
              onClick={() => navigate(`/provider/chat/${c.requestId}`)}
            >
              <div className="conv-left">
                <div className="service-tag">
                  {c.serviceType?.toUpperCase()}
                </div>
                <div className="user">{c.userName}</div>
              </div>

              <div className="conv-right">
                <div className="preview">
                  {c.lastMessage?.text
                    ? c.lastMessage.text.slice(0, 30)
                    : "No messages yet"}
                </div>
                <div className="status">{c.status}</div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
