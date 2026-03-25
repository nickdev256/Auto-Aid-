import React, { useEffect, useState, useRef, useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from "recharts";
import jsPDF from "jspdf";
import html2canvas from "html2canvas";
import "./Reports.css";

export default function Reports({ adminName = "Admin" }) {
  const navigate = useNavigate();
  const reportRef = useRef(null);

  const BASE =
    import.meta.env.VITE_API_URL?.replace(/\/$/, "") || "http://localhost:5001";

  const [stats, setStats] = useState({
    totalUsers: 0,
    totalProviders: 0,
    completedServices: 0,
    activeRequests: 0,
  });

  const [serviceRequests, setServiceRequests] = useState([]);
  const [pendingProviders, setPendingProviders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [exporting, setExporting] = useState(false);

  const COLORS = ["#2563eb", "#10b981", "#f59e0b", "#ef4444"];

  const fetchJson = async (path) => {
    const res = await fetch(`${BASE}${path}`, {
      method: "GET",
      credentials: "include",
    });

    if (res.status === 401) {
      throw new Error("SESSION_EXPIRED");
    }

    if (!res.ok) {
      const body = await res.text().catch(() => "");
      throw new Error(body || `Request failed: ${res.status}`);
    }

    return res.json();
  };

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const [statsRes, requestsRes, pendingRes] = await Promise.all([
        fetchJson("/api/admin/stats"),
        fetchJson("/api/admin/service-requests"),
        fetchJson("/api/admin/pending-providers"),
      ]);

      setStats({
        totalUsers: statsRes?.totalUsers ?? 0,
        totalProviders: statsRes?.totalProviders ?? 0,
        completedServices: statsRes?.completedServices ?? 0,
        activeRequests: statsRes?.activeRequests ?? 0,
      });

      setServiceRequests(Array.isArray(requestsRes) ? requestsRes : []);
      setPendingProviders(Array.isArray(pendingRes) ? pendingRes : []);
    } catch (err) {
      console.error("Reports load error:", err);

      setStats({
        totalUsers: 0,
        totalProviders: 0,
        completedServices: 0,
        activeRequests: 0,
      });
      setServiceRequests([]);
      setPendingProviders([]);

      if (err.message === "SESSION_EXPIRED") {
        setError("Session expired. Please login again.");
      } else {
        setError(err.message || "Failed to load reports");
      }
    } finally {
      setLoading(false);
    }
  }, [BASE]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const safeRequests = Array.isArray(serviceRequests) ? serviceRequests : [];
  const safePending = Array.isArray(pendingProviders) ? pendingProviders : [];

  const prettyStatus = (status = "") => {
    const s = String(status).toLowerCase();

    if (
      ["pending", "request_sent", "submitted", "awaiting_provider"].includes(s)
    ) {
      return "Pending";
    }

    if (
      ["assigned", "driver_assigned", "mechanic_assigned", "vendor_assigned"].includes(s)
    ) {
      return "Assigned";
    }

    if (
      ["in_progress", "on_the_way", "provider_on_the_way", "arrived"].includes(s)
    ) {
      return "In Progress";
    }

    if (["completed", "resolved", "delivered", "at_hospital"].includes(s)) {
      return "Completed";
    }

    if (["cancelled", "rejected", "declined"].includes(s)) {
      return "Cancelled";
    }

    return status || "Unknown";
  };

  const statusClass = (status = "") => {
    const s = String(status).toLowerCase();

    if (
      ["pending", "request_sent", "submitted", "awaiting_provider"].includes(s)
    ) {
      return "pending";
    }

    if (
      ["assigned", "driver_assigned", "mechanic_assigned", "vendor_assigned"].includes(s)
    ) {
      return "assigned";
    }

    if (
      ["in_progress", "on_the_way", "provider_on_the_way", "arrived"].includes(s)
    ) {
      return "active";
    }

    if (["completed", "resolved", "delivered", "at_hospital"].includes(s)) {
      return "completed";
    }

    if (["cancelled", "rejected", "declined"].includes(s)) {
      return "cancelled";
    }

    return "neutral";
  };

  const requestStatusData = useMemo(() => {
    return [
      {
        name: "Pending",
        value: safeRequests.filter((r) => statusClass(r.status) === "pending").length,
      },
      {
        name: "Assigned",
        value: safeRequests.filter((r) => statusClass(r.status) === "assigned").length,
      },
      {
        name: "In Progress",
        value: safeRequests.filter((r) => statusClass(r.status) === "active").length,
      },
      {
        name: "Completed",
        value: safeRequests.filter((r) => statusClass(r.status) === "completed").length,
      },
    ];
  }, [safeRequests]);

  const stackedBarData = useMemo(() => {
    return [
      { name: "Users", users: stats.totalUsers, providers: 0, completed: 0, active: 0 },
      { name: "Providers", users: 0, providers: stats.totalProviders, completed: 0, active: 0 },
      { name: "Completed", users: 0, providers: 0, completed: stats.completedServices, active: 0 },
      { name: "Active", users: 0, providers: 0, completed: 0, active: stats.activeRequests },
    ];
  }, [stats]);

  const reportStats = useMemo(() => {
    const pendingRequests = safeRequests.filter(
      (r) => statusClass(r.status) === "pending" || statusClass(r.status) === "assigned"
    ).length;

    const active = safeRequests.filter((r) => statusClass(r.status) === "active").length;
    const completed = safeRequests.filter((r) => statusClass(r.status) === "completed").length;
    const cancelled = safeRequests.filter((r) => statusClass(r.status) === "cancelled").length;

    return {
      totalRequests: safeRequests.length,
      pendingRequests,
      active,
      completed,
      cancelled,
    };
  }, [safeRequests]);

  const handleDownloadPDF = async () => {
    if (!reportRef.current) return;

    setExporting(true);
    try {
      const canvas = await html2canvas(reportRef.current, {
        scale: 2,
        useCORS: true,
        backgroundColor: "#ffffff",
      });

      const imgData = canvas.toDataURL("image/png");

      const pdf = new jsPDF("p", "mm", "a4");
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfHeight = (canvas.height * pdfWidth) / canvas.width;
      const pageHeight = pdf.internal.pageSize.getHeight();

      let heightLeft = pdfHeight;
      let position = 0;

      pdf.addImage(imgData, "PNG", 0, position, pdfWidth, pdfHeight);
      heightLeft -= pageHeight;

      while (heightLeft > 0) {
        position = heightLeft - pdfHeight;
        pdf.addPage();
        pdf.addImage(imgData, "PNG", 0, position, pdfWidth, pdfHeight);
        heightLeft -= pageHeight;
      }

      pdf.save(`AutoAid_Admin_Report_${new Date().toLocaleDateString()}.pdf`);
    } catch (e) {
      console.error("PDF export error:", e);
      alert("Failed to export PDF. Try again.");
    } finally {
      setExporting(false);
    }
  };

  if (loading) {
    return (
      <div className="reports-page">
        <main className="reports-container">
          <div className="loading-box">Loading reports...</div>
        </main>
      </div>
    );
  }

  return (
    <div className="reports-page">
      <main className="reports-container">
        <section className="reports-hero card-ui">
          <div>
            <div className="hero-badge">Admin / Reports</div>
            <h1>Reports & Analytics</h1>
            <p>
              Review live admin metrics, provider approval workload, service
              request performance, and export a polished PDF report.
            </p>
          </div>

          <div className="hero-actions">
            <button className="btn btn-light" onClick={() => navigate("/admin")} type="button">
              Back to Dashboard
            </button>
            <button className="btn btn-light" onClick={loadAll} type="button">
              Refresh Data
            </button>
            <button
              className="btn btn-primary"
              onClick={handleDownloadPDF}
              disabled={!!error || exporting}
              type="button"
            >
              {exporting ? "Exporting..." : "Download PDF"}
            </button>
          </div>
        </section>

        {error && <div className="error-box">{error}</div>}

        <div ref={reportRef} className="report-export-area">
          <section className="reports-meta card-ui">
            <div className="meta-left">
              <h2>AutoAid Admin Report</h2>
              <p>
                <strong>Admin:</strong> {adminName}
              </p>
              <p>
                <strong>Date:</strong> {new Date().toLocaleString()}
              </p>
            </div>

            <div className="meta-right">
              <span className="report-tag">Live System Snapshot</span>
            </div>
          </section>

          <section className="reports-stats">
            <div className="stat-box">
              <span>Total Users</span>
              <strong>{stats.totalUsers}</strong>
            </div>
            <div className="stat-box">
              <span>Total Providers</span>
              <strong>{stats.totalProviders}</strong>
            </div>
            <div className="stat-box">
              <span>Completed Services</span>
              <strong>{stats.completedServices}</strong>
            </div>
            <div className="stat-box">
              <span>Active Requests</span>
              <strong>{stats.activeRequests}</strong>
            </div>
            <div className="stat-box">
              <span>Pending Providers</span>
              <strong>{safePending.length}</strong>
            </div>
            <div className="stat-box">
              <span>Total Requests</span>
              <strong>{reportStats.totalRequests}</strong>
            </div>
          </section>

          <section className="charts-grid">
            <div className="card-ui chart-card">
              <div className="chart-head">
                <h3>System Metrics Comparison</h3>
                <p>Users, providers, completed services, and active requests</p>
              </div>

              <ResponsiveContainer width="100%" height={350}>
                <BarChart data={stackedBarData}>
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="users" stackId="a" fill="#2563eb" />
                  <Bar dataKey="providers" stackId="a" fill="#10b981" />
                  <Bar dataKey="completed" stackId="a" fill="#f59e0b" />
                  <Bar dataKey="active" stackId="a" fill="#ef4444" />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="card-ui chart-card">
              <div className="chart-head">
                <h3>Service Request Status Distribution</h3>
                <p>How requests are distributed across statuses</p>
              </div>

              <ResponsiveContainer width="100%" height={350}>
                <PieChart>
                  <Pie
                    data={requestStatusData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    outerRadius={110}
                    label
                  >
                    {requestStatusData.map((entry, index) => (
                      <Cell key={index} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Legend verticalAlign="bottom" />
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </section>

          <section className="reports-summary-grid">
            <div className="card-ui summary-card">
              <h3>Request Summary</h3>
              <div className="summary-list">
                <div className="summary-row">
                  <span>Pending / Assigned</span>
                  <strong>{reportStats.pendingRequests}</strong>
                </div>
                <div className="summary-row">
                  <span>In Progress</span>
                  <strong>{reportStats.active}</strong>
                </div>
                <div className="summary-row">
                  <span>Completed</span>
                  <strong>{reportStats.completed}</strong>
                </div>
                <div className="summary-row">
                  <span>Cancelled</span>
                  <strong>{reportStats.cancelled}</strong>
                </div>
              </div>
            </div>

            <div className="card-ui summary-card">
              <h3>Provider Approval Snapshot</h3>
              {safePending.length === 0 ? (
                <div className="empty-inline">No pending providers</div>
              ) : (
                <div className="summary-list">
                  <div className="summary-row">
                    <span>Pending Providers</span>
                    <strong>{safePending.length}</strong>
                  </div>
                  <div className="summary-row">
                    <span>Immediate Review Need</span>
                    <strong>{safePending.length > 0 ? "High" : "Low"}</strong>
                  </div>
                </div>
              )}
            </div>
          </section>

          <section className="card-ui table-card">
            <div className="table-head">
              <h3>Pending Providers</h3>
              <p>Providers waiting for approval</p>
            </div>

            {safePending.length === 0 ? (
              <div className="empty-box">No pending providers</div>
            ) : (
              <div className="table-wrap">
                <table className="styled-table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Email</th>
                      <th>Business Type</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {safePending.map((p) => (
                      <tr key={p._id || p.id}>
                        <td>{p.name || "—"}</td>
                        <td>{p.email || "—"}</td>
                        <td>{p.businessType || "—"}</td>
                        <td>
                          <span className={`pill ${statusClass(p.status)}`}>
                            {prettyStatus(p.status)}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="card-ui table-card">
            <div className="table-head">
              <h3>Service Requests Overview</h3>
              <p>Latest request entries and status overview</p>
            </div>

            {safeRequests.length === 0 ? (
              <div className="empty-box">No service requests yet</div>
            ) : (
              <div className="table-wrap">
                <table className="styled-table">
                  <thead>
                    <tr>
                      <th>User</th>
                      <th>Service Type</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {safeRequests.map((r) => (
                      <tr key={r._id || r.requestId || r.id}>
                        <td>{r.userName || "—"}</td>
                        <td>{r.serviceType || r.service || "—"}</td>
                        <td>
                          <span className={`pill ${statusClass(r.status)}`}>
                            {prettyStatus(r.status)}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}