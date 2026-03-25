import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import {
  FiArrowLeft,
  FiClock,
  FiMessageSquare,
  FiRefreshCw,
  FiUser,
} from "react-icons/fi";
import "./AdminChatList.css";

export default function AdminChatList() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    load();
    const iv = setInterval(load, 4000);
    return () => clearInterval(iv);
  }, []);

  async function load() {
    setLoading(true);
    try {
      const res = await axios.get(`/api/chat/all`);
      const list = res.data || [];

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
      setItems([]);
    } finally {
      setLoading(false);
    }
  }

  const formatService = (serviceType) =>
    String(serviceType || "general").replace(/_/g, " ").toUpperCase();

  const formatTime = (c) => {
    const raw =
      c?.lastMessage?.time ||
      c?.lastMessage?.createdAt ||
      c?.createdAt ||
      null;

    if (!raw) return "No time";
    const d = new Date(raw);
    if (Number.isNaN(d.getTime())) return "Unknown time";
    return d.toLocaleString();
  };

  return (
    <div className="acl-page">
      <main className="acl-container">
        <section className="acl-hero">
          <div className="acl-hero-left">
            <span className="acl-kicker">Admin / Conversations</span>
            <h1>All Conversations</h1>
            <p>
              Monitor user conversations, open active chat threads, and keep track
              of recent communication across AutoAid services.
            </p>

            <div className="acl-hero-mini">
              <div className="acl-mini-box">
                <span>Total Conversations</span>
                <strong>{items.length}</strong>
              </div>
              <div className="acl-mini-box">
                <span>Live Updates</span>
                <strong>Every 4s</strong>
              </div>
              <div className="acl-mini-box">
                <span>Chat Status</span>
                <strong>{loading ? "Loading" : "Ready"}</strong>
              </div>
            </div>
          </div>

          <div className="acl-hero-right">
            <button
              className="acl-btn acl-btn-light"
              onClick={() => navigate("/admin")}
              type="button"
            >
              <FiArrowLeft />
              <span>Back to Dashboard</span>
            </button>

            <button className="acl-btn acl-btn-primary" onClick={load} type="button">
              <FiRefreshCw />
              <span>Refresh</span>
            </button>
          </div>
        </section>

        <section className="acl-list-card">
          <div className="acl-list-head">
            <div>
              <span className="acl-section-label">Conversation Records</span>
              <h3>
                Showing {items.length} conversation{items.length === 1 ? "" : "s"}
              </h3>
            </div>
          </div>

          {loading ? (
            <div className="acl-empty-box">Loading conversations...</div>
          ) : items.length === 0 ? (
            <div className="acl-empty-box">No conversations yet</div>
          ) : (
            <div className="acl-list">
              {items.map((c) => (
                <div
                  key={c.requestId || c._id}
                  className="acl-card"
                  onClick={() => navigate(`/admin/chat/${c.requestId || c._id}`)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      navigate(`/admin/chat/${c.requestId || c._id}`);
                    }
                  }}
                >
                  <div className="acl-card-main">
                    <div className="acl-avatar">
                      <FiMessageSquare />
                    </div>

                    <div className="acl-card-info">
                      <div className="acl-title-row">
                        <h4>{c.userName || c.fromName || "Unknown"}</h4>
                        <span className="acl-service-badge">
                          {formatService(c.serviceType)}
                        </span>
                      </div>

                      <div className="acl-meta-row">
                        <span className="acl-meta-chip">
                          <FiUser />
                          <span>{c.status || c.type || "Conversation"}</span>
                        </span>

                        <span className="acl-meta-chip">
                          <FiClock />
                          <span>{formatTime(c)}</span>
                        </span>
                      </div>

                      <p className="acl-preview">
                        {c.lastMessage
                          ? c.lastMessage.text
                          : c.latestNote || "No messages"}
                      </p>
                    </div>
                  </div>

                  <div className="acl-card-action">
                    <button
                      type="button"
                      className="acl-open-btn"
                      onClick={(e) => {
                        e.stopPropagation();
                        navigate(`/admin/chat/${c.requestId || c._id}`);
                      }}
                    >
                      Open Chat
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}