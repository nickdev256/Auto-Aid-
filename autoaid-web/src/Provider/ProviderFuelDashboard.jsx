// src/Provider/ProviderFuelDashboard.jsx
import React, { useEffect, useRef, useState } from "react";
import axios from "axios";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { io } from "socket.io-client";
import "./ProviderFuelDashboard.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

// ✅ create socket ONCE (outside component) is okay
const socket = io(BASE, { transports: ["websocket"], withCredentials: true });

export default function ProviderFuelDashboard() {
  const { user, setUser, logout } = useAuth(); // ✅ needs setUser in AuthContext
  const navigate = useNavigate();
  const providerId = user?._id || user?.id;

  const [pending, setPending] = useState([]);
  const [ongoing, setOngoing] = useState([]);
  const [completed, setCompleted] = useState([]);
  const [loading, setLoading] = useState(true);

  const [showLogout, setShowLogout] = useState(false);
  const [toast, setToast] = useState(null);
  const [search, setSearch] = useState("");
  const [sortBy, setSortBy] = useState("newest");

  const prevPendingRef = useRef(0);
  const soundRef = useRef(null);

  /* VALIDATE ACCESS */
  useEffect(() => {
    if (!user) return navigate("/login");
    if (user.role !== "provider") return navigate("/login");
    if (user.businessType !== "fuel") {
      alert("This dashboard is for fuel providers only.");
      return navigate("/provider/dashboard");
    }
  }, [user, navigate]);

  // ✅ refresh current user (safe subscription fetch)
  const refreshMe = async () => {
    try {
      const res = await axios.get(`${BASE}/api/auth/me`, {
        withCredentials: true,
      });
      if (res.data?.user) setUser(res.data.user);
    } catch (err) {
      console.log("ME refresh failed:", err.response?.data || err.message);
      // If session expired, send to login
      // navigate("/login");
    }
  };

  /* LOAD FUEL REQUESTS */
  const loadRequests = async () => {
    try {
      const res = await axios.get(`${BASE}/api/fuel/byProvider/${providerId}`, {
        withCredentials: true,
      });

      const list = Array.isArray(res.data) ? res.data : [];

      const p = list.filter((r) => (r.status || "").toLowerCase() === "pending");
      const o = list.filter((r) => {
        const s = (r.status || "").toLowerCase();
        return s !== "pending" && s !== "completed";
      });
      const c = list.filter((r) => (r.status || "").toLowerCase() === "completed");

      setPending(p);
      setOngoing(o);
      setCompleted(c);

      // NEW REQUEST SOUND — compare pending count (not list length)
      if (p.length > prevPendingRef.current) {
        setToast({ type: "new", text: "New fuel request" });

        if (soundRef.current) {
          try {
            soundRef.current.currentTime = 0;
            soundRef.current.play();
          } catch {}
        }

        setTimeout(() => setToast(null), 2400);
      }

      prevPendingRef.current = p.length;
    } catch (err) {
      console.error("Load fuel requests error:", err.response?.data || err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!providerId) return;

    refreshMe();
    loadRequests();

    const iv = setInterval(() => {
      loadRequests();
      refreshMe();
    }, 8000);

    return () => clearInterval(iv);
  }, [providerId]);

  const isSubscribed = user?.subscription?.active === true;

  /* ACCEPT REQUEST */
  const acceptRequest = async (requestId) => {
    if (!isSubscribed) return navigate("/provider/subscription");

    try {
      await axios.post(
        `${BASE}/api/fuel/assign/${requestId}`,
        { providerId },
        { withCredentials: true }
      );

      loadRequests();
      setToast({ type: "success", text: "Request accepted" });
      setTimeout(() => setToast(null), 2000);
    } catch (err) {
      console.error("Accept error", err.response?.data || err.message);
      alert(err.response?.data?.message || "Failed to accept request");
    }
  };

  /* COMPLETE REQUEST */
  const completeRequest = async (requestId) => {
    try {
      await axios.patch(
        `${BASE}/api/fuel/${requestId}/status`,
        { status: "completed" },
        { withCredentials: true }
      );
      loadRequests();
    } catch (err) {
      alert(err.response?.data?.message || "Failed to complete request");
    }
  };

  /* SEND LIVE LOCATION */
  const sendLiveLocation = (requestId) => {
    if (!navigator.geolocation) return;

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        socket.emit("providerLocation", {
          providerId,
          requestId,
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
        });
      },
      (err) => console.warn("GPS blocked", err)
    );
  };

  /* FILTER + SORT */
  const matchSearch = (r) => {
    const q = search.toLowerCase();
    return (
      (r.userName || "").toLowerCase().includes(q) ||
      (r.address || "").toLowerCase().includes(q) ||
      (r.meta?.fuelType || "").toLowerCase().includes(q)
    );
  };

  const sortList = (list) =>
    list.slice().sort((a, b) =>
      sortBy === "newest"
        ? new Date(b.createdAt) - new Date(a.createdAt)
        : new Date(a.createdAt) - new Date(b.createdAt)
    );

  const visiblePending = sortList(pending.filter(matchSearch));
  const visibleOngoing = sortList(ongoing.filter(matchSearch));
  const visibleCompleted = sortList(completed.filter(matchSearch));

  if (loading) return <p className="loading">Loading…</p>;

  return (
    <div className="provider-fuel-upgraded has-anim">
      <audio ref={soundRef} src="/sounds/notification.mp3" preload="auto" />

      {toast && (
        <div className={`pf-toast ${toast.type === "new" ? "toast-new" : "toast-success"}`}>
          {toast.text}
        </div>
      )}

      {/* HEADER */}
      <header className="pf-top">
        <div className="pf-left">
          <h1 className="pf-title">Fuel Provider Dashboard</h1>
          <div className="pf-sub">
            Welcome, <strong>{user?.name}</strong>
          </div>
        </div>

        <div className="pf-controls">
          <input
            className="search-input"
            placeholder="Search requests..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          <select className="select" value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
            <option value="newest">Newest</option>
            <option value="oldest">Oldest</option>
          </select>

          <button className="btn settings-btn" onClick={() => navigate("/provider/settings")}>
            ⚙️
          </button>

          <button className="btn logout" onClick={() => setShowLogout(true)}>
            Logout
          </button>
        </div>
      </header>

      {/* SUBSCRIPTION WARNING (banner only) */}
      {!isSubscribed && (
        <div className="subscription-warning">
          🚫 You do not have an active subscription.
          <button className="subscribe-link" onClick={() => navigate("/provider/subscription")}>
            Activate Now →
          </button>
        </div>
      )}

      {/* COUNTERS */}
      <div className="badges">
        <div className="badge pending">Pending: {pending.length}</div>
        <div className="badge ongoing">Ongoing: {ongoing.length}</div>
        <div className="badge done">Completed: {completed.length}</div>
      </div>

      {/* MAIN GRID */}
      <main className="pf-main">
        {/* ------ PENDING ------ */}
        <section className="section-col">
          <h2 className="section-title">Pending Requests</h2>

          {visiblePending.length === 0 ? (
            <div className="empty-note">No pending requests.</div>
          ) : (
            <div className="list">
              {visiblePending.map((r) => {
                const id = r._id || r.requestId; // ✅ support both
                return (
                  <article key={id} className="req-card pulse">
                    <div className="req-left">
                      <div className="req-issue">Fuel: {r.meta?.fuelType}</div>
                      <div className="req-meta">{r.userName}</div>
                      <div className="req-small">
                        {r.meta?.quantityLitres} litres • {r.address}
                      </div>
                    </div>

                    <div className="req-right">
                      <div className="small-muted">{new Date(r.createdAt).toLocaleString()}</div>

                      <button
                        className={`btn accept ${!isSubscribed ? "disabled" : ""}`}
                        onClick={() => acceptRequest(id)}
                        disabled={!isSubscribed}
                      >
                        {isSubscribed ? "Accept" : "Subscribe to Accept"}
                      </button>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>

        {/* ------ ONGOING ------ */}
        <section className="section-col">
          <h2 className="section-title">Ongoing</h2>

          {visibleOngoing.length === 0 ? (
            <div className="empty-note">No ongoing deliveries.</div>
          ) : (
            <div className="list">
              {visibleOngoing.map((r) => {
                const id = r._id || r.requestId;
                return (
                  <article key={id} className="req-card">
                    <div className="req-left">
                      <div className="req-issue">{r.meta?.fuelType} Delivery</div>
                      <div className="req-meta">{r.userName}</div>
                      <div className="req-small">
                        {r.meta?.quantityLitres} litres • {r.address}
                      </div>
                    </div>

                    <div className="req-right">
                      <div className="small-muted">Status: {r.status}</div>

                      <div className="actions-col">
                        <button
                          className="btn outline"
                          onClick={() => {
                            sendLiveLocation(id);
                            navigate(`/fuel/map/${id}`);
                          }}
                        >
                          Navigate
                        </button>

                        <button className="btn outline" onClick={() => navigate(`/provider/chat/${id}`)}>
                          Chat
                        </button>

                        <button className="btn danger" onClick={() => completeRequest(id)}>
                          Complete
                        </button>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>

        {/* ------ COMPLETED ------ */}
        <section className="section-col">
          <h2 className="section-title">Completed</h2>

          {visibleCompleted.length === 0 ? (
            <div className="empty-note">No completed jobs yet.</div>
          ) : (
            <div className="list">
              {visibleCompleted.map((r) => {
                const id = r._id || r.requestId;
                return (
                  <article key={id} className="req-card completed">
                    <div className="req-left">
                      <div className="req-issue">{r.meta?.fuelType} Delivery</div>
                      <div className="req-meta">{r.userName}</div>
                      <div className="req-small">
                        {r.meta?.quantityLitres} litres • {r.address}
                      </div>
                    </div>

                    <div className="req-right">
                      <div className="small-muted">
                        Completed: {new Date(r.updatedAt || r.createdAt).toLocaleString()}
                      </div>

                      <button className="btn outline" onClick={() => navigate(`/fuel/map/${id}`)}>
                        View Route
                      </button>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>
      </main>

      {/* LOGOUT MODAL */}
      {showLogout && (
        <div className="modal-overlay" onClick={() => setShowLogout(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Confirm Logout</h3>
            <p>Are you sure you want to logout?</p>

            <div className="modal-actions">
              <button className="btn outline" onClick={() => setShowLogout(false)}>
                Cancel
              </button>

              <button
                className="btn danger"
                onClick={() => {
                  logout();
                  navigate("/login");
                }}
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}