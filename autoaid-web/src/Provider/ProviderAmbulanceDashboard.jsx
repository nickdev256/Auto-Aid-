// src/Provider/ProviderAmbulanceDashboard.jsx
import React, { useEffect, useRef, useState } from "react";
import axios from "axios";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import BusinessSettingsButton from "../components/BusinessSettingsButton";
import "./ProviderAmbulanceDashboard.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function ProviderAmbulanceDashboard() {
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

  // ------------------------------- VALIDATION -------------------------------
  useEffect(() => {
    if (!user) return navigate("/login");
    if (user.role !== "provider") return navigate("/login");
    if (user.businessType !== "ambulance") {
      alert("This dashboard is for ambulance providers only.");
      return navigate("/provider/dashboard");
    }
  }, [user, navigate]);

  // ✅ refresh current user subscription safely (no admin endpoint)
  const refreshMe = async () => {
    try {
      const res = await axios.get(`${BASE}/api/auth/me`, { withCredentials: true });
      if (res.data?.user) setUser(res.data.user);
    } catch (e) {
      // If not logged in anymore
      console.log("ME refresh failed:", e?.response?.data || e.message);
    }
  };

  // ------------------------------- LOAD REQUESTS ----------------------------
  const loadRequests = async () => {
    try {
      const res = await axios.get(`${BASE}/api/ambulance/byProvider/${providerId}`, {
        withCredentials: true,
      });

      const list = Array.isArray(res.data) ? res.data : [];

      const p = list.filter((r) => (r.status || "").toLowerCase() === "pending");
      const o = list.filter((r) => {
        const s = (r.status || "").toLowerCase();
        return (
          s === "assigned" ||
          s === "on_way" ||
          s === "on-the-way" ||
          s === "arrived" ||
          s === "transporting"
        );
      });
      const c = list.filter((r) => (r.status || "").toLowerCase() === "completed");

      setPending(p);
      setOngoing(o);
      setCompleted(c);

      // NEW REQUEST SOUND
      if (p.length > prevPendingRef.current) {
        if (soundRef.current) {
          try {
            soundRef.current.currentTime = 0;
            soundRef.current.play();
          } catch {}
        }
        setToast({ type: "new", text: "New Ambulance Request" });
        setTimeout(() => setToast(null), 2500);
      }

      prevPendingRef.current = p.length;
    } catch (err) {
      console.error("Ambulance load error:", err.response?.data || err.message);
    } finally {
      setLoading(false);
    }
  };

  // ------------------------------- INITIAL LOAD -----------------------------
  useEffect(() => {
    if (!providerId) return;

    refreshMe();        // ✅ get latest subscription state
    loadRequests();

    const iv = setInterval(() => {
      loadRequests();
      refreshMe();      // ✅ keeps subscription state up to date
    }, 8000);

    return () => clearInterval(iv);
  }, [providerId]);

  const isSubscribed = user?.subscription?.active === true;

  // ------------------------------- ACTIONS ----------------------------------
  const acceptRequest = async (r) => {
    if (!isSubscribed) return navigate("/provider/subscription");

    try {
      await axios.post(
        `${BASE}/api/ambulance/assign/${r._id}`,
        { providerId },
        { withCredentials: true }
      );

      setToast({ type: "success", text: "Request Accepted" });
      setTimeout(() => setToast(null), 2000);
      loadRequests();
    } catch {
      alert("Failed to accept request");
    }
  };

  const updateStatus = async (r, status) => {
    try {
      await axios.patch(
        `${BASE}/api/ambulance/${r._id}/status`,
        { status },
        { withCredentials: true }
      );
      loadRequests();
    } catch {
      alert("Failed to update status");
    }
  };

  const matchSearch = (r) => {
    const q = search.toLowerCase();
    return (
      (r.userName || "").toLowerCase().includes(q) ||
      (r.userPhone || "").toLowerCase().includes(q) ||
      (r.address || "").toLowerCase().includes(q)
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

  if (loading) return <div className="pg-loading">Loading...</div>;

  return (
    <div className="provider-fuel-upgraded has-anim">
      <audio ref={soundRef} src="/sounds/notification.mp3" preload="auto" />

      {/* Toast */}
      {toast && (
        <div className={`pf-toast ${toast.type === "new" ? "toast-new" : "toast-success"}`}>
          {toast.text}
        </div>
      )}

      {/* =============================== HEADER =============================== */}
      <header className="pf-top">
        <div className="pf-left">
          <h1 className="pf-title">Ambulance Provider Dashboard</h1>
          <div className="pf-sub">
            Welcome back, <strong>{user?.name}</strong>
          </div>
        </div>

        <div className="pf-controls">
          <input
            className="search-input"
            placeholder="Search user, phone, address..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          <select className="select" value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
            <option value="newest">Newest</option>
            <option value="oldest">Oldest</option>
          </select>

          <button className="btn settings-btn" onClick={() => navigate("/provider/settings")}>
            <BusinessSettingsButton />
          </button>

          <button className="btn logout" onClick={() => setShowLogout(true)}>
            Logout
          </button>
        </div>
      </header>

      {/* ✅ CTA BANNER ONLY (no subscription page in dashboard) */}
      {!isSubscribed && (
        <div className="subscription-warning">
          🚫 You do not have an active subscription.
          <button className="subscribe-link" onClick={() => navigate("/provider/subscription")}>
            Activate Now →
          </button>
        </div>
      )}

      {/* ======================== BADGES ======================== */}
      <div className="badges">
        <div className="badge pending">Pending: {pending.length}</div>
        <div className="badge ongoing">Ongoing: {ongoing.length}</div>
        <div className="badge done">Completed: {completed.length}</div>
      </div>

      {/* ======================== 3 COLUMN LAYOUT ======================== */}
      <main className="pf-main">
        {/* Pending */}
        <section className="section-col">
          <h2 className="section-title">Pending Requests</h2>

          {visiblePending.length === 0 ? (
            <div className="empty-note">No pending ambulance requests.</div>
          ) : (
            <div className="list">
              {visiblePending.map((r) => (
                <article key={r._id} className="req-card pulse">
                  <div className="req-left">
                    <div className="req-issue">{r.issue || "Medical Emergency"}</div>
                    <div className="req-meta">
                      {r.userName} • {r.userPhone || "No phone"}
                    </div>
                    <div className="req-small">{r.address || "Unknown location"}</div>
                  </div>

                  <div className="req-right">
                    <div className="small-muted">
                      Created: {new Date(r.createdAt).toLocaleString()}
                    </div>

                    <button
                      className={`btn accept ${!isSubscribed ? "disabled" : ""}`}
                      disabled={!isSubscribed}
                      onClick={() => acceptRequest(r)}
                    >
                      {isSubscribed ? "Accept" : "Subscribe to Accept"}
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        {/* Ongoing */}
        <section className="section-col">
          <h2 className="section-title">Ongoing</h2>

          {visibleOngoing.length === 0 ? (
            <div className="empty-note">No ongoing ambulance jobs.</div>
          ) : (
            <div className="list">
              {visibleOngoing.map((r) => (
                <article key={r._id} className="req-card">
                  <div className="req-left">
                    <div className="req-issue">{r.issue || "Emergency Transport"}</div>
                    <div className="req-meta">
                      {r.userName} • {r.userPhone || "No phone"}
                    </div>
                    <div className="req-small">{r.address || "Unknown location"}</div>
                  </div>

                  <div className="req-right">
                    <div className="small-muted">Status: {r.status}</div>

                    <div className="actions-col">
                      {(r.status || "").toLowerCase() === "assigned" && (
                        <button className="btn" onClick={() => updateStatus(r, "on_way")}>
                          On The Way
                        </button>
                      )}

                      {(r.status || "").toLowerCase() === "on_way" && (
                        <button className="btn" onClick={() => updateStatus(r, "arrived")}>
                          Arrived
                        </button>
                      )}

                      <button className="btn outline" onClick={() => navigate(`/ambulance/map/${r._id}`)}>
                        Navigate
                      </button>

                      <button className="btn outline" onClick={() => navigate(`/provider/chat/${r._id}`)}>
                        Chat
                      </button>

                      {(r.status || "").toLowerCase() !== "completed" && (
                        <button className="btn danger" onClick={() => updateStatus(r, "completed")}>
                          Complete
                        </button>
                      )}
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        {/* Completed */}
        <section className="section-col">
          <h2 className="section-title">Completed</h2>

          {visibleCompleted.length === 0 ? (
            <div className="empty-note">No completed jobs yet.</div>
          ) : (
            <div className="list">
              {visibleCompleted.map((r) => (
                <article key={r._id} className="req-card completed">
                  <div className="req-left">
                    <div className="req-issue">{r.issue || "Completed Transport"}</div>
                    <div className="req-meta">
                      {r.userName} • {r.userPhone || "No phone"}
                    </div>
                    <div className="req-small">{r.address}</div>
                  </div>

                  <div className="req-right">
                    <div className="small-muted">
                      Completed: {new Date(r.updatedAt || r.createdAt).toLocaleString()}
                    </div>

                    <button className="btn outline" onClick={() => navigate(`/ambulance/map/${r._id}`)}>
                      View Route
                    </button>
                  </div>
                </article>
              ))}
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
              <button className="btn danger" onClick={logout}>
                Logout
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}