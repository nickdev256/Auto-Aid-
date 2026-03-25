import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import AdminSidebar from "../../components/AdminSidebar";
import {
  getAdminStats,
  getPendingProviders,
  getServiceRequests,
} from "../../services/api";
import "./AdminDashboard.css";

export default function AdminDashboard() {
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const [stats, setStats] = useState(null);
  const [pendingProviders, setPendingProviders] = useState([]);
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [reqFrom, setReqFrom] = useState("all");
  const [searchTerm, setSearchTerm] = useState("");

  const navigate = useNavigate();

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    try {
      const statsData = await getAdminStats();
      const pending = await getPendingProviders();
      const reqs = await getServiceRequests(reqFrom);

      setStats(statsData || {});
      setPendingProviders(Array.isArray(pending) ? pending.slice(0, 6) : []);
      setRequests(Array.isArray(reqs) ? reqs.slice(0, 8) : []);
    } catch (err) {
      console.error("Dashboard load error:", err);
      setStats({});
      setPendingProviders([]);
      setRequests([]);
    } finally {
      setLoading(false);
    }
  }, [reqFrom]);

  useEffect(() => {
    loadDashboard();
    const iv = setInterval(loadDashboard, 30000);
    return () => clearInterval(iv);
  }, [loadDashboard]);

  const toId = (r) => r?.requestId || r?._id || r?.id;

  const prettyFrom = (r) => {
    const v = (r?.requestedFrom || "").toLowerCase();
    if (v === "android") return "Android App";
    if (v === "web") return "Web";
    return "Web";
  };

  const prettyStatus = (status = "") => {
    const s = status.toLowerCase();

    if (
      [
        "pending",
        "request_sent",
        "submitted",
        "awaiting_provider",
        "awaiting_approval",
      ].includes(s)
    ) {
      return "Pending";
    }

    if (
      [
        "assigned",
        "driver_assigned",
        "mechanic_assigned",
        "vendor_assigned",
      ].includes(s)
    ) {
      return "Assigned";
    }

    if (
      [
        "accepted",
        "in_progress",
        "on_the_way",
        "provider_on_the_way",
        "towing_on_the_way",
        "ambulance_on_the_way",
        "fuel_on_the_way",
      ].includes(s)
    ) {
      return "In Progress";
    }

    if (["arrived"].includes(s)) {
      return "Arrived";
    }

    if (
      ["completed", "delivered", "at_hospital", "resolved"].includes(s)
    ) {
      return "Completed";
    }

    if (["cancelled", "rejected", "declined"].includes(s)) {
      return "Cancelled";
    }

    return status || "Unknown";
  };

  const getStatusClass = (status = "") => {
    const s = status.toLowerCase();

    if (
      [
        "pending",
        "request_sent",
        "submitted",
        "awaiting_provider",
        "awaiting_approval",
      ].includes(s)
    ) {
      return "status pending";
    }

    if (
      [
        "assigned",
        "driver_assigned",
        "mechanic_assigned",
        "vendor_assigned",
      ].includes(s)
    ) {
      return "status assigned";
    }

    if (
      [
        "accepted",
        "in_progress",
        "on_the_way",
        "provider_on_the_way",
        "towing_on_the_way",
        "ambulance_on_the_way",
        "fuel_on_the_way",
        "arrived",
      ].includes(s)
    ) {
      return "status active";
    }

    if (
      ["completed", "delivered", "at_hospital", "resolved"].includes(s)
    ) {
      return "status completed";
    }

    if (["cancelled", "rejected", "declined"].includes(s)) {
      return "status cancelled";
    }

    return "status neutral";
  };

  const quickCounts = useMemo(() => {
    let web = 0;
    let android = 0;

    for (const r of requests) {
      if ((r?.requestedFrom || "").toLowerCase() === "android") android += 1;
      else web += 1;
    }

    return { web, android };
  }, [requests]);

  const filteredPendingProviders = useMemo(() => {
    if (!searchTerm.trim()) return pendingProviders;

    return pendingProviders.filter((p) =>
      `${p?.name || ""} ${p?.businessType || ""} ${p?.role || ""}`
        .toLowerCase()
        .includes(searchTerm.toLowerCase())
    );
  }, [pendingProviders, searchTerm]);

  const filteredRequests = useMemo(() => {
    if (!searchTerm.trim()) return requests;

    return requests.filter((r) =>
      `${r?.serviceType || ""} ${r?.status || ""} ${r?.requestedFrom || ""}`
        .toLowerCase()
        .includes(searchTerm.toLowerCase())
    );
  }, [requests, searchTerm]);

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/login");
  };

  if (loading && !stats) {
    return (
      <div className="admin-shell">
        <AdminSidebar open={sidebarOpen} setOpen={setSidebarOpen} />
        <div className={`admin-main ${sidebarOpen ? "expanded" : "collapsed"}`}>
          <div className="dashboard-loading-wrap">
            <div className="dashboard-loader" />
            <p>Loading dashboard...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-shell">
      <AdminSidebar open={sidebarOpen} setOpen={setSidebarOpen} />

      <div className={`admin-main ${sidebarOpen ? "expanded" : "collapsed"}`}>
        <main className="dashboard-page">
          <section className="dashboard-hero glass-card">
            <div className="dashboard-hero-left">
              <span className="hero-badge">AutoAid Admin Panel</span>
              <h1>Admin Dashboard</h1>
              <p>
                Monitor users, providers, requests, approvals, and system
                performance in one place.
              </p>
            </div>

            <div className="dashboard-hero-right">
              <div className="top-search-wrap">
                <input
                  type="text"
                  className="top-search"
                  placeholder="Search requests, providers, service type..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>

              <div className="hero-actions">
                <button
                  className="secondary-btn"
                  onClick={() => loadDashboard()}
                  type="button"
                >
                  Refresh
                </button>
                <button
                  className="primary-btn danger"
                  onClick={handleLogout}
                  type="button"
                >
                  Sign out
                </button>
              </div>
            </div>
          </section>

          <section className="stats-grid">
            <div
              className="stat-card users"
              onClick={() => navigate("/admin/users")}
            >
              <div className="stat-card-top">
                <span className="stat-icon">👥</span>
                <span className="stat-tag">Users</span>
              </div>
              <h2>{stats?.totalUsers ?? 0}</h2>
              <p>Total registered users in the system</p>
            </div>

            <div
              className="stat-card providers"
              onClick={() => navigate("/admin/providers")}
            >
              <div className="stat-card-top">
                <span className="stat-icon">🏪</span>
                <span className="stat-tag">Providers</span>
              </div>
              <h2>{stats?.totalProviders ?? 0}</h2>
              <p>All service providers on the platform</p>
            </div>

            <div
              className="stat-card active"
              onClick={() => navigate("/admin/requests")}
            >
              <div className="stat-card-top">
                <span className="stat-icon">⚡</span>
                <span className="stat-tag">Active</span>
              </div>
              <h2>{stats?.activeRequests ?? 0}</h2>
              <p>Requests that are currently ongoing</p>
            </div>

            <div
              className="stat-card completed"
              onClick={() => navigate("/admin/requests?filter=completed")}
            >
              <div className="stat-card-top">
                <span className="stat-icon">✅</span>
                <span className="stat-tag">Completed</span>
              </div>
              <h2>{stats?.completedServices ?? 0}</h2>
              <p>Successfully completed service requests</p>
            </div>

            <div
              className="stat-card web"
              onClick={() => {
                setReqFrom("web");
                navigate("/admin/requests?from=web");
              }}
            >
              <div className="stat-card-top">
                <span className="stat-icon">🌐</span>
                <span className="stat-tag">Web</span>
              </div>
              <h2>{stats?.webRequests ?? quickCounts.web ?? 0}</h2>
              <p>Requests submitted from the website</p>
            </div>

            <div
              className="stat-card app"
              onClick={() => {
                setReqFrom("android");
                navigate("/admin/requests?from=android");
              }}
            >
              <div className="stat-card-top">
                <span className="stat-icon">📱</span>
                <span className="stat-tag">App</span>
              </div>
              <h2>{stats?.androidRequests ?? quickCounts.android ?? 0}</h2>
              <p>Requests submitted from the Android app</p>
            </div>
          </section>

          <section className="overview-grid">
            <div className="wide-card glass-card">
              <div className="section-head">
                <div>
                  <h3>System Summary</h3>
                  <p>Quick operational overview</p>
                </div>
              </div>

              <div className="mini-stats-grid">
                <div className="mini-stat-box">
                  <span>Pending Providers</span>
                  <strong>{pendingProviders.length}</strong>
                </div>
                <div className="mini-stat-box">
                  <span>Recent Requests</span>
                  <strong>{requests.length}</strong>
                </div>
                <div className="mini-stat-box">
                  <span>Web Ratio</span>
                  <strong>
                    {quickCounts.web + quickCounts.android > 0
                      ? `${Math.round(
                          (quickCounts.web / (quickCounts.web + quickCounts.android)) *
                            100
                        )}%`
                      : "0%"}
                  </strong>
                </div>
                <div className="mini-stat-box">
                  <span>App Ratio</span>
                  <strong>
                    {quickCounts.web + quickCounts.android > 0
                      ? `${Math.round(
                          (quickCounts.android /
                            (quickCounts.web + quickCounts.android)) *
                            100
                        )}%`
                      : "0%"}
                  </strong>
                </div>
              </div>
            </div>

            <div className="wide-card glass-card">
              <div className="section-head">
                <div>
                  <h3>Quick Actions</h3>
                  <p>Shortcuts to main admin modules</p>
                </div>
              </div>

              <div className="quick-actions-grid">
                <button onClick={() => navigate("/admin/providers")}>
                  Provider Management
                </button>
                <button onClick={() => navigate("/admin/users")}>
                  User Management
                </button>
                <button onClick={() => navigate("/admin/requests")}>
                  Service Requests
                </button>
                <button onClick={() => navigate("/admin/subscriptions")}>
                  Subscriptions
                </button>
                <button onClick={() => navigate("/admin/reports")}>
                  Reports
                </button>
                <button onClick={() => navigate("/admin/settings")}>
                  Settings
                </button>
              </div>
            </div>
          </section>

          <section className="content-grid">
            <div className="panel-card glass-card">
              <div className="section-head">
                <div>
                  <h3>Pending Approvals</h3>
                  <p>Providers waiting for admin review</p>
                </div>
                <button
                  className="text-btn"
                  onClick={() => navigate("/admin/providers")}
                  type="button"
                >
                  View all
                </button>
              </div>

              {filteredPendingProviders.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-icon">📭</div>
                  <h4>No pending providers</h4>
                  <p>Everything looks good right now.</p>
                </div>
              ) : (
                <div className="list-wrap">
                  {filteredPendingProviders.map((p) => (
                    <div className="list-row provider-row" key={p?._id}>
                      <div className="row-main">
                        <div className="avatar-badge">
                          {(p?.name || "P").charAt(0).toUpperCase()}
                        </div>

                        <div className="row-details">
                          <h4>{p?.name || "Unnamed Provider"}</h4>
                          <p>{p?.businessType || p?.role || "Provider"}</p>
                        </div>
                      </div>

                      <div className="row-side">
                        <span className="status pending">Pending</span>
                        <button
                          className="action-btn"
                          onClick={() => navigate("/admin/providers")}
                          type="button"
                        >
                          Review
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="panel-card glass-card">
              <div className="section-head requests-head">
                <div>
                  <h3>Recent Requests</h3>
                  <p>Latest requests across all services</p>
                </div>

                <div className="filter-group">
                  <button
                    className={reqFrom === "all" ? "filter-btn active" : "filter-btn"}
                    onClick={() => setReqFrom("all")}
                    type="button"
                  >
                    All
                  </button>
                  <button
                    className={reqFrom === "web" ? "filter-btn active" : "filter-btn"}
                    onClick={() => setReqFrom("web")}
                    type="button"
                  >
                    Web
                  </button>
                  <button
                    className={
                      reqFrom === "android" ? "filter-btn active" : "filter-btn"
                    }
                    onClick={() => setReqFrom("android")}
                    type="button"
                  >
                    App
                  </button>
                </div>
              </div>

              {filteredRequests.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-icon">🚗</div>
                  <h4>No recent requests</h4>
                  <p>There are no recent request records to display.</p>
                </div>
              ) : (
                <div className="table-like-list">
                  {filteredRequests.map((r) => (
                    <div className="request-item" key={toId(r)}>
                      <div className="request-item-left">
                        <div className="request-service-icon">🛠️</div>

                        <div className="request-meta">
                          <h4>{r?.serviceType || "Service Request"}</h4>
                          <div className="request-subline">
                            <span className={getStatusClass(r?.status)}>
                              {prettyStatus(r?.status)}
                            </span>
                            <span className="dot">•</span>
                            <span className="source-pill">{prettyFrom(r)}</span>
                          </div>
                        </div>
                      </div>

                      <div className="request-item-right">
                        <span className="request-date">
                          {r?.createdAt
                            ? new Date(r.createdAt).toLocaleDateString()
                            : "N/A"}
                        </span>

                        <button
                          className="action-btn"
                          onClick={() =>
                            navigate(`/admin/requests?view=${toId(r)}`)
                          }
                          type="button"
                        >
                          View
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <div className="panel-footer">
                <button
                  className="text-btn"
                  onClick={() =>
                    navigate(
                      reqFrom === "all"
                        ? "/admin/requests"
                        : `/admin/requests?from=${reqFrom}`
                    )
                  }
                  type="button"
                >
                  View all requests
                </button>
              </div>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}