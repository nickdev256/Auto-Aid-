// src/pages/Admin/AdminDashboard.jsx
import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import AdminSidebar from "../../components/AdminSidebar";
import {
  getAdminStats,
  getPendingProviders,
  getServiceRequests,
} from "../../services/api";
import "./AdminDashboard.css";

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [pendingProviders, setPendingProviders] = useState([]);
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);

  // ✅ NEW: filter for requests (all | web | android)
  const [reqFrom, setReqFrom] = useState("all");

  const navigate = useNavigate();

  const loadDashboard = async () => {
    setLoading(true);
    try {
      const statsData = await getAdminStats();
      const pending = await getPendingProviders();

      // ✅ NEW: pass filter to backend
      const reqs = await getServiceRequests(reqFrom); // "all" | "web" | "android"

      setStats(statsData || {});
      setPendingProviders((pending && pending.slice(0, 6)) || []);
      setRequests((reqs && reqs.slice(0, 8)) || []);
    } catch (err) {
      console.error("Dashboard load error:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
    const iv = setInterval(loadDashboard, 30 * 1000);
    return () => clearInterval(iv);
    // ✅ re-load when filter changes
  }, [reqFrom]);

  const toId = (r) => r.requestId || r._id || r.id;

  const prettyFrom = (r) => {
    const v = (r?.requestedFrom || "").toLowerCase();
    if (v === "android") return "Android";
    return "Web"; // default
  };

  // ✅ helper counts for quick UI (works even if backend stats not updated)
  const quickCounts = useMemo(() => {
    let web = 0;
    let android = 0;
    for (const r of requests) {
      if ((r?.requestedFrom || "").toLowerCase() === "android") android += 1;
      else web += 1;
    }
    return { web, android };
  }, [requests]);

  if (loading || !stats) {
    return (
      <div className="admin-preview-root">
        <AdminSidebar />
        <main className="admin-dashboard-container">
          <div className="loading-shell">Loading dashboard...</div>
        </main>
      </div>
    );
  }

  return (
    <div className="admin-preview-root">
      <AdminSidebar />

      <main className="admin-dashboard-container" role="main">
        <div className="top-row">
          <div className="page-title">
            <h1>Admin Dashboard</h1>
            <p className="subtitle">Overview & quick actions</p>
          </div>

          <div className="top-actions">
            <button
              className="outline-btn"
              onClick={() => {
                localStorage.removeItem("token");
                navigate("/login");
              }}
            >
              Sign out
            </button>
          </div>
        </div>

        {/* ✅ STATS GRID */}
        <section className="stats-grid">
          <div
            className="stat-card card-acrylic users"
            onClick={() => navigate("/admin/providers")}
          >
            <div className="stat-left">
              <h2>{stats.totalUsers ?? 0}</h2>
              <p>Users</p>
            </div>
            <div className="stat-right">👥</div>
          </div>

          <div
            className="stat-card card-acrylic providers"
            onClick={() => navigate("/admin/providers")}
          >
            <div className="stat-left">
              <h2>{stats.totalProviders ?? 0}</h2>
              <p>Providers</p>
            </div>
            <div className="stat-right">🏪</div>
          </div>

          <div
            className="stat-card card-acrylic active"
            onClick={() => navigate("/admin/requests")}
          >
            <div className="stat-left">
              <h2>{stats.activeRequests ?? 0}</h2>
              <p>Active</p>
            </div>
            <div className="stat-right">⚡</div>
          </div>

          <div
            className="stat-card card-acrylic completed"
            onClick={() => navigate("/admin/requests?filter=completed")}
          >
            <div className="stat-left">
              <h2>{stats.completedServices ?? 0}</h2>
              <p>Completed</p>
            </div>
            <div className="stat-right">✅</div>
          </div>

          {/* ✅ NEW: Web requests stat (if backend provides webRequests/androidRequests, use them.
              Otherwise show quickCounts from the recent list.) */}
          <div
            className="stat-card card-acrylic"
            onClick={() => {
              setReqFrom("web");
              navigate("/admin/requests?from=web");
            }}
            style={{ cursor: "pointer" }}
          >
            <div className="stat-left">
              <h2>{stats.webRequests ?? quickCounts.web ?? 0}</h2>
              <p>Web Requests</p>
            </div>
            <div className="stat-right">🌐</div>
          </div>

          <div
            className="stat-card card-acrylic"
            onClick={() => {
              setReqFrom("android");
              navigate("/admin/requests?from=android");
            }}
            style={{ cursor: "pointer" }}
          >
            <div className="stat-left">
              <h2>{stats.androidRequests ?? quickCounts.android ?? 0}</h2>
              <p>App Requests</p>
            </div>
            <div className="stat-right">📱</div>
          </div>
        </section>

        <div className="row-2">
          {/* ✅ PENDING */}
          <div className="pending-card card-acrylic">
            <div className="card-header">
              <h3>Pending Approvals</h3>
              <button
                className="link-btn"
                onClick={() => navigate("/admin/providers")}
              >
                View all
              </button>
            </div>

            {pendingProviders.length === 0 ? (
              <div className="empty">No pending providers</div>
            ) : (
              <ul className="pending-list">
                {pendingProviders.map((p) => (
                  <li key={p._id} className="pending-row">
                    <div>
                      <strong>{p.name}</strong>
                      <div className="small">{p.businessType || p.role}</div>
                    </div>

                    <div className="row-actions">
                      <button
                        className="ghost-btn"
                        onClick={() => navigate(`/admin/providers`)}
                      >
                        View
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* ✅ REQUESTS */}
          <div className="requests-card card-acrylic">
            <div className="card-header" style={{ gap: 12 }}>
              <h3 style={{ marginRight: "auto" }}>Recent Requests</h3>

              {/* ✅ NEW: filter pills */}
              <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <button
                  className={reqFrom === "all" ? "outline-btn" : "ghost-btn"}
                  onClick={() => setReqFrom("all")}
                  type="button"
                >
                  All
                </button>
                <button
                  className={reqFrom === "web" ? "outline-btn" : "ghost-btn"}
                  onClick={() => setReqFrom("web")}
                  type="button"
                >
                  Web
                </button>
                <button
                  className={reqFrom === "android" ? "outline-btn" : "ghost-btn"}
                  onClick={() => setReqFrom("android")}
                  type="button"
                >
                  App
                </button>
              </div>

              <button
                className="link-btn"
                onClick={() =>
                  navigate(
                    reqFrom === "all"
                      ? "/admin/requests"
                      : `/admin/requests?from=${reqFrom}`
                  )
                }
              >
                View all
              </button>
            </div>

            {requests.length === 0 ? (
              <div className="empty">No recent requests</div>
            ) : (
              <ul className="requests-list">
                {requests.map((r) => (
                  <li key={toId(r)} className="request-row">
                    <div className="left">
                      <strong>{r.serviceType}</strong>
                      <div className="small">
                        {r.status}
                        {" • "}
                        <span
                          style={{
                            padding: "2px 8px",
                            borderRadius: 999,
                            border: "1px solid rgba(255,255,255,0.18)",
                            marginLeft: 6,
                            fontSize: 12,
                          }}
                          title="Where the request came from"
                        >
                          {prettyFrom(r)}
                        </span>
                      </div>
                    </div>

                    <div className="right">
                      <span className="date">
                        {new Date(r.createdAt).toLocaleDateString()}
                      </span>

                      <button
                        className="view-btn"
                        onClick={() =>
                          navigate(`/admin/requests?view=${toId(r)}`)
                        }
                      >
                        View
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>

        <div className="admin-menu-buttons">
          <button onClick={() => navigate("/admin/subscriptions")}>
            Manage Subscriptions
          </button>
          <button onClick={() => navigate("/admin/reports")}>View Reports</button>
          <button onClick={() => navigate("/admin/settings")}>Settings</button>
        </div>
      </main>
    </div>
  );
}