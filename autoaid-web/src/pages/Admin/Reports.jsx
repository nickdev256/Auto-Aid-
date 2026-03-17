import React, { useEffect, useState, useRef, useMemo } from "react";
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

  const BASE = "http://localhost:5001";

  // ✅ keep safe defaults
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalProviders: 0,
    completedServices: 0,
    activeRequests: 0,
  });

  const [serviceRequests, setServiceRequests] = useState([]); // ✅ always array
  const [pendingProviders, setPendingProviders] = useState([]); // ✅ always array
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // AUTOAID COLOR PALETTE
  const COLORS = ["#0088FE", "#00C49F", "#FFBB28", "#FF8042"];

  // ================= HELPERS =================
  const fetchJson = async (path) => {
    const res = await fetch(`${BASE}${path}`, {
      method: "GET",
      credentials: "include", // ✅ REQUIRED for cookie auth
    });

    // ✅ Session expired / not logged in
    if (res.status === 401) {
      throw new Error("SESSION_EXPIRED");
    }

    // ✅ Handle other failures
    if (!res.ok) {
      const body = await res.text().catch(() => "");
      throw new Error(body || `Request failed: ${res.status}`);
    }

    return res.json();
  };

  // ================= LOAD DATA =================
  const loadAll = async () => {
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

      // Keep state safe so UI never crashes
      setStats({ totalUsers: 0, totalProviders: 0, completedServices: 0, activeRequests: 0 });
      setServiceRequests([]);
      setPendingProviders([]);

      if (err.message === "SESSION_EXPIRED") {
        setError("Session expired. Please login again.");
        // Optional redirect:
        // navigate("/admin/login");
      } else {
        setError(err.message || "Failed to load reports");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ✅ Always use safe arrays
  const safeRequests = Array.isArray(serviceRequests) ? serviceRequests : [];
  const safePending = Array.isArray(pendingProviders) ? pendingProviders : [];

  // ================= DATA MAP =================
  const requestStatusData = useMemo(() => {
    return [
      { name: "Pending", value: safeRequests.filter((r) => r.status === "pending").length },
      { name: "Assigned", value: safeRequests.filter((r) => r.status === "assigned").length },
      { name: "Completed", value: safeRequests.filter((r) => r.status === "completed").length },
    ];
  }, [safeRequests]);

  // STACKED CHART — using REAL LIVE data
  const stackedBarData = useMemo(() => {
    return [
      { name: "Users", users: stats.totalUsers, providers: 0, completed: 0 },
      { name: "Providers", users: 0, providers: stats.totalProviders, completed: 0 },
      { name: "Completed Services", users: 0, providers: 0, completed: stats.completedServices },
    ];
  }, [stats]);

  // ================= PDF EXPORT =================
  const handleDownloadPDF = async () => {
    if (!reportRef.current) return;

    try {
      const canvas = await html2canvas(reportRef.current, { scale: 2 });
      const imgData = canvas.toDataURL("image/png");

      const pdf = new jsPDF("p", "mm", "a4");
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfHeight = (canvas.height * pdfWidth) / canvas.width;

      pdf.addImage(imgData, "PNG", 0, 0, pdfWidth, pdfHeight);
      pdf.save(`AutoAid_Admin_Report_${new Date().toLocaleDateString()}.pdf`);
    } catch (e) {
      console.error("PDF export error:", e);
      alert("Failed to export PDF. Try again.");
    }
  };

  // ================= UI =================
  if (loading) return <p>Loading admin dashboard...</p>;

  return (
    <div className="reports-container">
      <button className="back-btn" onClick={() => navigate(-1)}>
        ⬅ Back
      </button>

      <h1>Admin Reports & Analytics</h1>

      {error && (
        <div className="error-box" style={{ marginBottom: 16 }}>
          {error}
        </div>
      )}

      <button className="btn-download" onClick={handleDownloadPDF} disabled={!!error}>
        Download PDF
      </button>

      <div ref={reportRef} className="report-content">
        <h2 className="report-title">AutoAid Admin Report</h2>
        <p>
          <strong>Admin:</strong> {adminName}
        </p>
        <p>
          <strong>Date:</strong> {new Date().toLocaleString()}
        </p>

        {/* SUMMARY CARDS */}
        <div className="cards-container">
          <div className="card">
            <h3>Total Users</h3>
            <p>{stats.totalUsers}</p>
          </div>

          <div className="card">
            <h3>Total Providers</h3>
            <p>{stats.totalProviders}</p>
          </div>

          <div className="card">
            <h3>Completed Services</h3>
            <p>{stats.completedServices}</p>
          </div>

          <div className="card">
            <h3>Pending Providers</h3>
            <p>{safePending.length}</p>
          </div>
        </div>

        <hr className="divider" />

        {/* CHARTS SIDE BY SIDE */}
        <div className="charts-container">
          {/* STACKED BAR CHART */}
          <div className="chart large-chart">
            <h3>System Metrics Comparison (Stacked Bar Chart)</h3>
            <p className="chart-desc">Users vs Providers vs Completed Services</p>

            <ResponsiveContainer width="100%" height={350}>
              <BarChart data={stackedBarData}>
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Legend />

                <Bar dataKey="users" stackId="a" fill="#0088FE" />
                <Bar dataKey="providers" stackId="a" fill="#00C49F" />
                <Bar dataKey="completed" stackId="a" fill="#FFBB28" />
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* PIE CHART */}
          <div className="chart large-chart">
            <h3>Service Requests Status Distribution (Pie Chart)</h3>
            <p className="chart-desc">Proportion of service request statuses</p>

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
        </div>

        <hr className="divider" />

        {/* TABLES */}
        <section className="table-section">
          <h2>Pending Providers</h2>

          {safePending.length === 0 ? (
            <p>No pending providers</p>
          ) : (
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
                    <td>{p.status || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section className="table-section">
          <h2>Service Requests Overview</h2>

          {safeRequests.length === 0 ? (
            <p>No service requests yet</p>
          ) : (
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
                    <td>{r.status || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>

      {/* Optional refresh */}
      <div style={{ marginTop: 16 }}>
        <button className="btn-download" onClick={loadAll}>
          Refresh Data
        </button>
      </div>
    </div>
  );
}