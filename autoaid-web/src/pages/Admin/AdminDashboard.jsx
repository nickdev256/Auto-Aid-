import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
  FiUsers,
  FiShield,
  FiClock,
  FiCheckCircle,
  FiGlobe,
  FiSmartphone,
  FiRefreshCw,
  FiLogOut,
  FiTool,
  FiTruck,
  FiDroplet,
  FiActivity,
  FiArrowRight,
  FiFileText,
  FiSettings,
  FiCreditCard,
  FiTrendingUp,
  FiLayers,
  FiCommand,
  FiSearch,
} from "react-icons/fi";
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
    if (v === "web") return "Web Platform";
    return "Web Platform";
  };

  const prettyStatus = (status = "") => {
    const s = status.toLowerCase();

    if (
      ["pending", "request_sent", "submitted", "awaiting_provider", "awaiting_approval"].includes(s)
    ) return "Pending";

    if (
      ["assigned", "driver_assigned", "mechanic_assigned", "vendor_assigned"].includes(s)
    ) return "Assigned";

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
    ) return "In Progress";

    if (["arrived"].includes(s)) return "Arrived";

    if (["completed", "delivered", "at_hospital", "resolved"].includes(s)) return "Completed";

    if (["cancelled", "rejected", "declined"].includes(s)) return "Cancelled";

    return status || "Unknown";
  };

  const getStatusClass = (status = "") => {
    const s = status.toLowerCase();

    if (
      ["pending", "request_sent", "submitted", "awaiting_provider", "awaiting_approval"].includes(s)
    ) return "status pending";

    if (
      ["assigned", "driver_assigned", "mechanic_assigned", "vendor_assigned"].includes(s)
    ) return "status assigned";

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
    ) return "status active";

    if (["completed", "delivered", "at_hospital", "resolved"].includes(s)) return "status completed";

    if (["cancelled", "rejected", "declined"].includes(s)) return "status cancelled";

    return "status neutral";
  };

  const getServiceMeta = (serviceType = "") => {
    const s = serviceType.toLowerCase();

    if (s.includes("fuel")) {
      return {
        label: "Fuel",
        icon: <FiDroplet />,
        className: "service-fuel",
      };
    }

    if (s.includes("tow")) {
      return {
        label: "Towing",
        icon: <FiTruck />,
        className: "service-towing",
      };
    }

    if (s.includes("ambulance")) {
      return {
        label: "Ambulance",
        icon: <FiActivity />,
        className: "service-ambulance",
      };
    }

    if (s.includes("garage") || s.includes("mechanic") || s.includes("repair")) {
      return {
        label: "Garage",
        icon: <FiTool />,
        className: "service-garage",
      };
    }

    return {
      label: serviceType || "Service",
      icon: <FiFileText />,
      className: "service-default",
    };
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

  const statCards = [
    {
      key: "users",
      label: "Users",
      value: stats?.totalUsers ?? 0,
      text: "Registered platform users",
      icon: <FiUsers />,
      className: "users",
      onClick: () => navigate("/admin/users"),
    },
    {
      key: "providers",
      label: "Providers",
      value: stats?.totalProviders ?? 0,
      text: "All service providers",
      icon: <FiShield />,
      className: "providers",
      onClick: () => navigate("/admin/providers"),
    },
    {
      key: "active",
      label: "Live Requests",
      value: stats?.activeRequests ?? 0,
      text: "Requests in progress",
      icon: <FiClock />,
      className: "active",
      onClick: () => navigate("/admin/requests"),
    },
    {
      key: "completed",
      label: "Completed",
      value: stats?.completedServices ?? 0,
      text: "Successfully finished",
      icon: <FiCheckCircle />,
      className: "completed",
      onClick: () => navigate("/admin/requests?filter=completed"),
    },
    {
      key: "web",
      label: "Web Requests",
      value: stats?.webRequests ?? quickCounts.web ?? 0,
      text: "Requests from website",
      icon: <FiGlobe />,
      className: "web",
      onClick: () => {
        setReqFrom("web");
        navigate("/admin/requests?from=web");
      },
    },
    {
      key: "app",
      label: "App Requests",
      value: stats?.androidRequests ?? quickCounts.android ?? 0,
      text: "Requests from Android app",
      icon: <FiSmartphone />,
      className: "app",
      onClick: () => {
        setReqFrom("android");
        navigate("/admin/requests?from=android");
      },
    },
  ];

  if (loading && !stats) {
    return (
      <div className="admin-layout">
        <AdminSidebar open={sidebarOpen} setOpen={setSidebarOpen} />
        <div className="admin-content">
          <div className="dashboard-loading-wrap">
            <div className="dashboard-loader" />
            <p>Loading dashboard...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-layout">
      <AdminSidebar open={sidebarOpen} setOpen={setSidebarOpen} />

      <div className={`admin-content ${sidebarOpen ? "sidebar-open" : "sidebar-closed"}`}>
        <main className="autoaid-dashboard">
          <section className="command-hero">
            <div className="command-hero-left">
              <div className="eyebrow-badge">
                <FiCommand />
                <span>AutoAid Control Center</span>
              </div>

              <h1>Admin Dashboard</h1>
              <p>
                A fresh operations dashboard for managing requests, providers,
                approvals, usage channels, and platform performance from one
                central place.
              </p>

              <div className="hero-mini-metrics">
                <div className="hero-mini-card">
                  <span className="mini-label">Pending Approvals</span>
                  <strong>{pendingProviders.length}</strong>
                </div>
                <div className="hero-mini-card">
                  <span className="mini-label">Recent Requests</span>
                  <strong>{requests.length}</strong>
                </div>
                <div className="hero-mini-card">
                  <span className="mini-label">Channel Split</span>
                  <strong>{quickCounts.web}:{quickCounts.android}</strong>
                </div>
              </div>
            </div>

            <div className="command-hero-right">
              <div className="dashboard-search">
                <FiSearch />
                <input
                  type="text"
                  placeholder="Search providers, requests, status, service..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>

              <div className="hero-action-row">
                <button className="hero-btn ghost" onClick={loadDashboard} type="button">
                  <FiRefreshCw />
                  <span>Refresh</span>
                </button>

                <button
                  className="hero-btn danger"
                  onClick={handleLogout}
                  type="button"
                >
                  <FiLogOut />
                  <span>Sign out</span>
                </button>
              </div>

              <div className="hero-highlight-card">
                <div className="hero-highlight-top">
                  <span className="highlight-icon">
                    <FiTrendingUp />
                  </span>
                  <div>
                    <h3>System Health</h3>
                    <p>Operational snapshot</p>
                  </div>
                </div>

                <div className="health-bars">
                  <div className="health-line">
                    <span>Web Traffic</span>
                    <strong>
                      {quickCounts.web + quickCounts.android > 0
                        ? `${Math.round(
                            (quickCounts.web / (quickCounts.web + quickCounts.android)) * 100
                          )}%`
                        : "0%"}
                    </strong>
                  </div>
                  <div className="health-line">
                    <span>App Traffic</span>
                    <strong>
                      {quickCounts.web + quickCounts.android > 0
                        ? `${Math.round(
                            (quickCounts.android / (quickCounts.web + quickCounts.android)) * 100
                          )}%`
                        : "0%"}
                    </strong>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section className="command-stats-grid">
            {statCards.map((card) => (
              <button
                key={card.key}
                className={`command-stat-card ${card.className}`}
                onClick={card.onClick}
                type="button"
              >
                <div className="command-stat-top">
                  <span className="command-stat-icon">{card.icon}</span>
                  <span className="command-stat-label">{card.label}</span>
                </div>

                <div className="command-stat-value">{card.value}</div>
                <p>{card.text}</p>
              </button>
            ))}
          </section>

          <section className="command-overview-grid">
            <div className="dashboard-panel summary-panel">
              <div className="panel-head">
                <div>
                  <span className="panel-kicker">Overview</span>
                  <h3>Operational Summary</h3>
                </div>
              </div>

              <div className="summary-grid">
                <div className="summary-box">
                  <span>Total Users</span>
                  <strong>{stats?.totalUsers ?? 0}</strong>
                </div>
                <div className="summary-box">
                  <span>Total Providers</span>
                  <strong>{stats?.totalProviders ?? 0}</strong>
                </div>
                <div className="summary-box">
                  <span>Active Requests</span>
                  <strong>{stats?.activeRequests ?? 0}</strong>
                </div>
                <div className="summary-box">
                  <span>Completed</span>
                  <strong>{stats?.completedServices ?? 0}</strong>
                </div>
              </div>
            </div>

            <div className="dashboard-panel actions-panel">
              <div className="panel-head">
                <div>
                  <span className="panel-kicker">Shortcuts</span>
                  <h3>Quick Actions</h3>
                </div>
              </div>

              <div className="shortcut-grid">
                <button onClick={() => navigate("/admin/providers")} type="button">
                  <FiShield />
                  <span>Providers</span>
                </button>
                <button onClick={() => navigate("/admin/users")} type="button">
                  <FiUsers />
                  <span>Users</span>
                </button>
                <button onClick={() => navigate("/admin/requests")} type="button">
                  <FiLayers />
                  <span>Requests</span>
                </button>
                <button onClick={() => navigate("/admin/subscriptions")} type="button">
                  <FiCreditCard />
                  <span>Subscriptions</span>
                </button>
                <button onClick={() => navigate("/admin/reports")} type="button">
                  <FiFileText />
                  <span>Reports</span>
                </button>
                <button onClick={() => navigate("/admin/settings")} type="button">
                  <FiSettings />
                  <span>Settings</span>
                </button>
              </div>
            </div>
          </section>

          <section className="dashboard-main-grid">
            <div className="dashboard-panel approvals-panel">
              <div className="panel-head">
                <div>
                  <span className="panel-kicker">Approvals</span>
                  <h3>Pending Providers</h3>
                  <p>Providers waiting for verification or review</p>
                </div>

                <button
                  className="panel-link-btn"
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
                  <p>Everything looks clear at the moment.</p>
                </div>
              ) : (
                <div className="provider-list">
                  {filteredPendingProviders.map((p) => (
                    <div className="provider-item" key={p?._id}>
                      <div className="provider-item-left">
                        <div className="provider-avatar">
                          {(p?.name || "P").charAt(0).toUpperCase()}
                        </div>

                        <div className="provider-info">
                          <h4>{p?.name || "Unnamed Provider"}</h4>
                          <p>{p?.businessType || p?.role || "Provider"}</p>
                        </div>
                      </div>

                      <div className="provider-item-right">
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

            <div className="dashboard-panel requests-panel">
              <div className="panel-head requests-panel-head">
                <div>
                  <span className="panel-kicker">Requests</span>
                  <h3>Recent Service Activity</h3>
                  <p>Latest requests across AutoAid services</p>
                </div>

                <div className="request-filter-group">
                  <button
                    className={reqFrom === "all" ? "request-filter-btn active" : "request-filter-btn"}
                    onClick={() => setReqFrom("all")}
                    type="button"
                  >
                    All
                  </button>
                  <button
                    className={reqFrom === "web" ? "request-filter-btn active" : "request-filter-btn"}
                    onClick={() => setReqFrom("web")}
                    type="button"
                  >
                    Web
                  </button>
                  <button
                    className={reqFrom === "android" ? "request-filter-btn active" : "request-filter-btn"}
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
                  <p>There are no request records to show right now.</p>
                </div>
              ) : (
                <div className="request-list">
                  {filteredRequests.map((r) => {
                    const meta = getServiceMeta(r?.serviceType || "");

                    return (
                      <div className="request-card-row" key={toId(r)}>
                        <div className="request-card-left">
                          <div className={`request-type-icon ${meta.className}`}>
                            {meta.icon}
                          </div>

                          <div className="request-card-info">
                            <div className="request-name-line">
                              <h4>{meta.label}</h4>
                              <span className={`service-chip ${meta.className}`}>
                                {meta.label}
                              </span>
                            </div>

                            <div className="request-meta-line">
                              <span className={getStatusClass(r?.status)}>
                                {prettyStatus(r?.status)}
                              </span>
                              <span className="meta-separator">•</span>
                              <span className="source-chip">{prettyFrom(r)}</span>
                            </div>
                          </div>
                        </div>

                        <div className="request-card-right">
                          <span className="request-date">
                            {r?.createdAt
                              ? new Date(r.createdAt).toLocaleDateString()
                              : "N/A"}
                          </span>

                          <button
                            className="action-btn"
                            onClick={() => navigate(`/admin/requests?view=${toId(r)}`)}
                            type="button"
                          >
                            <span>View</span>
                            <FiArrowRight />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              <div className="panel-footer">
                <button
                  className="panel-link-btn"
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