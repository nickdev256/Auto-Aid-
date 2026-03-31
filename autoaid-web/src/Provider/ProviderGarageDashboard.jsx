import React, { useEffect, useRef, useState, useMemo } from "react";
import axios from "axios";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import "./ProviderGarageDashboard.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function ProviderGarageDashboard() {
  const { user, setUser, logout } = useAuth();
  const navigate = useNavigate();

  const providerId = user?._id || user?.id || null;

  const [pending, setPending] = useState([]);
  const [ongoing, setOngoing] = useState([]);
  const [completed, setCompleted] = useState([]);
  const [loading, setLoading] = useState(true);

  const [toast, setToast] = useState(null);
  const [search, setSearch] = useState("");
  const [sortBy, setSortBy] = useState("newest");
  const [showLogout, setShowLogout] = useState(false);

  const prevPendingRef = useRef(0);
  const soundRef = useRef(null);
  const intervalRef = useRef(null);

  const api = useMemo(
    () =>
      axios.create({
        baseURL: BASE,
        withCredentials: true,
      }),
    []
  );

  useEffect(() => {
    if (!user) {
      navigate("/login");
      return;
    }

    if (user.role !== "provider") {
      navigate("/login");
      return;
    }

    if ((user.businessType || "").toLowerCase() !== "garage") {
      navigate("/provider/dashboard");
    }
  }, [user, navigate]);

  const refreshMe = async () => {
    try {
      const res = await api.get("/api/auth/me");
      if (res.data?.user) setUser(res.data.user);
    } catch (err) {
      console.log("Session expired");
    }
  };

  const loadRequests = async () => {
    if (!providerId) return;

    try {
      const [assignedRes, pendingRes] = await Promise.all([
        api.get(`/api/garage/byProvider/${providerId}`),
        api.get(`/api/garage/available`),
      ]);

      const assigned = Array.isArray(assignedRes.data) ? assignedRes.data : [];
      const pendingList = Array.isArray(pendingRes.data) ? pendingRes.data : [];

      setPending(pendingList);
      setOngoing(
        assigned.filter((r) => (r.status || "").toLowerCase() !== "completed")
      );
      setCompleted(
        assigned.filter((r) => (r.status || "").toLowerCase() === "completed")
      );

      if (pendingList.length > prevPendingRef.current) {
        setToast({ type: "new", text: "New garage request" });

        if (soundRef.current) {
          try {
            soundRef.current.currentTime = 0;
            soundRef.current.play();
          } catch {
            // ignore autoplay errors
          }
        }

        setTimeout(() => setToast(null), 2000);
      }

      prevPendingRef.current = pendingList.length;
    } catch (err) {
      console.error("Load requests error:", err.response?.data || err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!providerId) return;

    refreshMe();
    loadRequests();

    intervalRef.current = setInterval(() => {
      refreshMe();
      loadRequests();
    }, 8000);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [providerId]);

  const isSubscribed = user?.subscription?.active === true;

  const acceptRequest = async (id) => {
    if (!isSubscribed) {
      navigate("/provider/subscription");
      return;
    }

    try {
      await api.post(`/api/garage/${id}/assign`, { providerId });

      setToast({ type: "success", text: "Request accepted" });
      setTimeout(() => setToast(null), 1500);

      loadRequests();
    } catch (err) {
      alert(err.response?.data?.message || "Failed to accept request");
    }
  };

  const updateStatus = async (id, status) => {
    try {
      await api.patch(`/api/garage/${id}/status`, { status });
      loadRequests();
    } catch (err) {
      alert(err.response?.data?.message || "Failed to update status");
    }
  };

  const matchSearch = (r) => {
    if (!search) return true;
    const q = search.toLowerCase();

    return (
      (r.userName || "").toLowerCase().includes(q) ||
      (r.vehicleInfo || "").toLowerCase().includes(q) ||
      (r.address || "").toLowerCase().includes(q) ||
      (r.issue || "").toLowerCase().includes(q)
    );
  };

  const sortList = (list) =>
    list
      .filter(Boolean)
      .slice()
      .sort((a, b) =>
        sortBy === "newest"
          ? new Date(b.createdAt) - new Date(a.createdAt)
          : new Date(a.createdAt) - new Date(b.createdAt)
      );

  const visiblePending = sortList(pending.filter(matchSearch));
  const visibleOngoing = sortList(ongoing.filter(matchSearch));
  const visibleCompleted = sortList(completed.filter(matchSearch));

  if (loading) return <p className="pg-loading">Loading dashboard...</p>;

  return (
    <div className="provider-garage-upgraded">
      <audio ref={soundRef} src="/sounds/notification.mp3" preload="auto" />

      {toast && <div className={`pg-toast ${toast.type}`}>{toast.text}</div>}

      <header className="pg-top">
        <div>
          <h1>Garage Dashboard</h1>
          <p>
            Welcome back, <strong>{user?.name}</strong>
          </p>
        </div>

        <div className="pg-controls">
          <input
            placeholder="Search..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
            <option value="newest">Newest</option>
            <option value="oldest">Oldest</option>
          </select>

          <button onClick={() => navigate("/provider/settings")}>⚙ Settings</button>
          <button onClick={() => setShowLogout(true)}>Logout</button>
        </div>
      </header>

      {!isSubscribed && (
        <div className="subscription-warning">
          Subscription inactive
          <button onClick={() => navigate("/provider/subscription")}>
            Activate
          </button>
        </div>
      )}

      <div className="badges">
        <div>Pending: {pending.length}</div>
        <div>Ongoing: {ongoing.length}</div>
        <div>Completed: {completed.length}</div>
      </div>

      <main className="pg-main">
        <section>
          <h2>Pending Requests</h2>
          {visiblePending.length === 0 && <p>No pending requests</p>}
          {visiblePending.map((r) => {
            const id = r._id || r.requestId;
            return (
              <div key={id} className="req-card">
                <div>
                  <b>{r.issue || "Garage Help Needed"}</b>
                  <p>{r.userName}</p>
                  <small>{r.address}</small>
                </div>
                <button disabled={!isSubscribed} onClick={() => acceptRequest(id)}>
                  Accept
                </button>
              </div>
            );
          })}
        </section>

        <section>
          <h2>Ongoing</h2>
          {visibleOngoing.length === 0 && <p>No ongoing requests</p>}
          {visibleOngoing.map((r) => {
            const id = r._id || r.requestId;
            return (
              <div key={id} className="req-card">
                <div>
                  <b>{r.issue}</b>
                  <p>Status: {r.status}</p>
                </div>

                <button onClick={() => updateStatus(id, "completed")}>
                  Complete
                </button>
              </div>
            );
          })}
        </section>

        <section>
          <h2>Completed</h2>
          {visibleCompleted.length === 0 && <p>No completed requests</p>}
          {visibleCompleted.map((r) => {
            const id = r._id || r.requestId;
            return (
              <div key={id} className="req-card done">
                <b>{r.issue}</b>
              </div>
            );
          })}
        </section>
      </main>

      {showLogout && (
        <div className="modal-overlay" onClick={() => setShowLogout(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Logout?</h3>
            <button onClick={() => setShowLogout(false)}>Cancel</button>
            <button
              onClick={() => {
                logout();
                navigate("/login");
              }}
            >
              Logout
            </button>
          </div>
        </div>
      )}
    </div>
  );
}