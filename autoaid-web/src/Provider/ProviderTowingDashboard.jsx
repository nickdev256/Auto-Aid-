// src/Provider/ProviderTowingDashboard.jsx
import React, { useEffect, useRef, useState } from "react";
import axios from "axios";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import BusinessSettingsButton from "../components/BusinessSettingsButton";
import "./ProviderTowingDashboard.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function ProviderTowingDashboard() {
  const { user, setUser, logout } = useAuth(); // ✅ needs setUser in AuthContext
  const navigate = useNavigate();
  const providerId = user?._id || user?.id;

  // request groups
  const [pending, setPending] = useState([]);
  const [ongoing, setOngoing] = useState([]);
  const [completed, setCompleted] = useState([]);

  // ui
  const [loading, setLoading] = useState(true);
  const [showLogout, setShowLogout] = useState(false);
  const [q, setQ] = useState("");
  const [sortBy, setSortBy] = useState("newest");

  const soundRef = useRef(null);
  const prevPendingRef = useRef(0);
  const [toast, setToast] = useState(null);

  const [earnings, setEarnings] = useState({ total: 0, month: 0, count: 0 });

  const getId = (r) => r.requestId || r._id || r.id;

  // ✅ refresh current user (subscription status)
  const refreshMe = async () => {
    try {
      const res = await axios.get(`${BASE}/api/auth/me`, { withCredentials: true });
      if (res.data?.user) setUser(res.data.user);
    } catch (err) {
      console.log("ME refresh failed:", err.response?.data || err.message);
      // If session expired:
      // navigate("/login");
    }
  };

  // Load Earnings
  const loadEarnings = async (assignedList = []) => {
    try {
      const res = await axios.get(`${BASE}/api/providers/${providerId}/earnings`, {
        withCredentials: true,
      });
      if (res?.data) {
        setEarnings(res.data);
        return;
      }
    } catch {}

    // fallback
    const rate = 5000;
    const n = assignedList.filter((r) => String((r.status || "").toLowerCase()) === "completed")
      .length;

    setEarnings({
      total: n * rate,
      month: Math.round(n * rate * 0.32),
      count: n,
    });
  };

  // Load requests
  const loadRequests = async () => {
    setLoading(true);
    try {
      // pending -> only unassigned within radius (backend handles providerId)
      const pendingRes = await axios.get(`${BASE}/api/towing/available/${providerId}`, {
        withCredentials: true,
      });

      const providerRes = await axios.get(`${BASE}/api/towing/byProvider/${providerId}`, {
        withCredentials: true,
      });

      const pendingList = Array.isArray(pendingRes.data) ? pendingRes.data : [];
      const assignedList = Array.isArray(providerRes.data) ? providerRes.data : [];

      setPending(pendingList);

      setOngoing(
        assignedList.filter((r) => String(r.status || "").toLowerCase() !== "completed")
      );

      setCompleted(
        assignedList.filter((r) => String(r.status || "").toLowerCase() === "completed")
      );

      await loadEarnings(assignedList);

      // new request sound (compare pending counts)
      if (pendingList.length > prevPendingRef.current) {
        try {
          if (soundRef.current) {
            soundRef.current.currentTime = 0;
            soundRef.current.play();
          }
        } catch {}

        setToast({
          type: "new",
          text: `New towing request (${pendingList.length - prevPendingRef.current})`,
        });
        setTimeout(() => setToast(null), 2500);
      }

      prevPendingRef.current = pendingList.length;
    } catch (err) {
      console.error("Towing load error:", err.response?.data || err.message);
    } finally {
      setLoading(false);
    }
  };

  /* VALIDATE ACCESS */
  useEffect(() => {
    if (!user) return navigate("/login");
    if (user.role !== "provider") return navigate("/login");
    if (user.businessType !== "towing") {
      alert("This dashboard is for towing providers only.");
      return navigate("/provider/dashboard");
    }
  }, [user, navigate]);

  // initial load
  useEffect(() => {
    if (!providerId) return;

    refreshMe();
    loadRequests();

    const iv = setInterval(() => {
      loadRequests();
      refreshMe();
    }, 8000);

    return () => clearInterval(iv);
    // eslint-disable-next-line
  }, [providerId]);

  const isSubscribed = user?.subscription?.active === true;

  // Accept
  const acceptRequest = async (id) => {
    if (!isSubscribed) return navigate("/provider/subscription");

    try {
      await axios.post(
        `${BASE}/api/towing/assign/${id}`,
        { providerId },
        { withCredentials: true }
      );

      loadRequests();
      setToast({ type: "success", text: "Accepted request" });
      setTimeout(() => setToast(null), 1800);
    } catch (err) {
      if (err?.response?.status === 409) {
        alert("Request already accepted by another provider.");
      } else {
        alert(err?.response?.data?.message || "Failed to accept request.");
      }
      loadRequests();
    }
  };

  // Update status
  const updateStatus = async (id, status) => {
    try {
      await axios.patch(
        `${BASE}/api/towing/${id}/status`,
        { status },
        { withCredentials: true }
      );
      loadRequests();
      setToast({ type: "success", text: `Status updated: ${status}` });
      setTimeout(() => setToast(null), 2000);
    } catch (err) {
      alert(err?.response?.data?.message || "Failed to update");
    }
  };

  // Logout
  const confirmLogout = () => {
    logout();
    navigate("/login");
  };

  // Filtering
  const matchQ = (r) => {
    const ql = q.toLowerCase();
    return (
      (r.userName || "").toLowerCase().includes(ql) ||
      (r.vehicleInfo || "").toLowerCase().includes(ql) ||
      (r.address || "").toLowerCase().includes(ql)
    );
  };

  const sortList = (list) =>
    list
      .filter(matchQ)
      .slice()
      .sort((a, b) =>
        sortBy === "newest"
          ? new Date(b.createdAt) - new Date(a.createdAt)
          : new Date(a.createdAt) - new Date(b.createdAt)
      );

  const visiblePending = sortList(pending);
  const visibleOngoing = sortList(ongoing);
  const visibleCompleted = sortList(completed);

  if (loading) return <p className="loading">Loading towing dashboard…</p>;

  return (
    <div className="provider-towing-upgraded has-anim">
      <audio ref={soundRef} src="/sounds/notification.mp3" preload="auto" />

      {toast && (
        <div className={`pt-toast ${toast.type === "new" ? "toast-new" : "toast-success"}`}>
          {toast.text}
        </div>
      )}

      {/* header */}
      <header className="pt-top">
        <div className="pt-left">
          <h1 className="pt-title">Towing Provider Dashboard</h1>
          <div className="pt-sub">
            Welcome back, <strong>{user?.name}</strong>
          </div>
        </div>

        <div className="pt-controls">
          <input
            className="search-input"
            placeholder="Search requests..."
            value={q}
            onChange={(e) => setQ(e.target.value)}
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

      {/* subscription banner only */}
      {!isSubscribed && (
        <div className="subscription-warning">
          🚫 You do not have an active subscription.
          <button className="subscribe-link" onClick={() => navigate("/provider/subscription")}>
            Activate Now →
          </button>
        </div>
      )}

      <div className="badges">
        <div className="badge pending">Pending: {pending.length}</div>
        <div className="badge ongoing">Ongoing: {ongoing.length}</div>
        <div className="badge done">Completed: {completed.length}</div>
      </div>

      <main className="pt-main">
        {/* Pending */}
        <section className="section-col">
          <h2 className="section-title">Pending Requests</h2>
          {visiblePending.length === 0 ? (
            <div className="empty-note">No pending towing requests.</div>
          ) : (
            <div className="list">
              {visiblePending.map((r) => {
                const id = getId(r);
                return (
                  <article key={id} className="req-card pulse">
                    <div className="req-left">
                      <div className="req-issue">{r.issue || "Towing Request"}</div>
                      <div className="req-meta">{r.userName}</div>
                      <div className="req-small">
                        {r.vehicleInfo} • {r.address}
                      </div>
                    </div>

                    <div className="req-right">
                      <div className="small-muted">
                        Created: {new Date(r.createdAt).toLocaleString()}
                      </div>

                      <button
                        className={`btn accept ${isSubscribed ? "" : "disabled"}`}
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

        {/* Ongoing */}
        <section className="section-col">
          <h2 className="section-title">Ongoing</h2>
          {visibleOngoing.length === 0 ? (
            <div className="empty-note">No ongoing towing.</div>
          ) : (
            <div className="list">
              {visibleOngoing.map((r) => {
                const id = getId(r);
                const st = String(r.status || "").toLowerCase();

                return (
                  <article key={id} className="req-card">
                    <div className="req-left">
                      <div className="req-issue">{r.issue}</div>
                      <div className="req-meta">{r.userName}</div>
                      <div className="req-small">
                        {r.vehicleInfo} • {r.address}
                      </div>
                    </div>

                    <div className="req-right">
                      <div className="small-muted">Status: {r.status}</div>

                      <div className="actions-col">
                        {st === "assigned" && (
                          <button className="btn" onClick={() => updateStatus(id, "on_way")}>
                            Mark On The Way
                          </button>
                        )}

                        {st === "on_way" && (
                          <button className="btn danger" onClick={() => updateStatus(id, "completed")}>
                            Complete
                          </button>
                        )}

                        <button
                          className="btn outline"
                          onClick={() => navigate(`/provider/map/${id}?service=towing`)}
                        >
                          Navigate
                        </button>

                        <button className="btn outline" onClick={() => navigate(`/provider/chat/${id}`)}>
                          Chat
                        </button>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>

        {/* Completed */}
        <section className="section-col">
          <h2 className="section-title">Completed</h2>
          {visibleCompleted.length === 0 ? (
            <div className="empty-note">No completed towing yet.</div>
          ) : (
            <div className="list">
              {visibleCompleted.map((r) => {
                const id = getId(r);
                return (
                  <article key={id} className="req-card completed">
                    <div className="req-left">
                      <div className="req-issue">{r.issue}</div>
                      <div className="req-meta">{r.userName}</div>
                      <div className="req-small">
                        {r.vehicleInfo} • {r.address}
                      </div>
                    </div>

                    <div className="req-right">
                      <div className="small-muted">
                        Completed: {new Date(r.updatedAt || r.createdAt).toLocaleString()}
                      </div>

                      <button
                        className="btn outline"
                        onClick={() => navigate(`/provider/map/${id}?service=towing`)}
                      >
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
              <button className="btn danger" onClick={confirmLogout}>
                Logout
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}