import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./AdminChatList.css"; // optional styling

export default function AdminChatList() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  // Auto load with polling (can replace with socket.io later)
  useEffect(() => {
    load();
    const iv = setInterval(load, 4000);
    return () => clearInterval(iv);
  }, []);

  async function load() {
    setLoading(true);
    try {
      // EXPECTS: 
      // [{ requestId, serviceType, userName, status, lastMessage, createdAt }]
      const res = await axios.get(`/api/chat/all`);
      const list = res.data || [];

      // Sort by lastMessage time OR createdAt
      list.sort((a, b) => {
        const ta = a.lastMessage
          ? new Date(a.lastMessage.time || a.lastMessage.createdAt)
          : new Date(a.createdAt || 0);

        const tb = b.lastMessage
          ? new Date(b.lastMessage.time || b.lastMessage.createdAt)
          : new Date(b.createdAt || 0);

        return tb - ta;
      });

      setItems(list);
    } catch (err) {
      console.error("admin chat list load", err);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="conv-root">
      <h2>All Conversations</h2>

      {loading ? (
        <p>Loading…</p>
      ) : items.length === 0 ? (
        <p>No conversations yet</p>
      ) : (
        <div className="conv-list">
          {items.map((c) => (
            <div
              key={c.requestId || c._id}
              className="conv-card"
              onClick={() => navigate(`/admin/chat/${c.requestId || c._id}`)}
            >
              <div className="conv-left">
                <div className="service">
                  {(c.serviceType || "GENERAL").toUpperCase()}
                </div>
                <div className="user">
                  {c.userName || c.fromName || "Unknown"}
                </div>
              </div>

              <div className="conv-right">
                <div className="preview">
                  {c.lastMessage
                    ? c.lastMessage.text
                    : c.latestNote || "No messages"}
                </div>
                <div className="status">{c.status || c.type || ""}</div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
