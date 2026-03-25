import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import "./Requests.css";

const API_BASE =
  import.meta.env.VITE_API_URL?.replace(/\/$/, "") || "http://localhost:5001";

export default function Requests() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [from, setFrom] = useState("all");
  const [search, setSearch] = useState("");

  const navigate = useNavigate();
  const location = useLocation();

  const viewId = new URLSearchParams(location.search).get("view");

  const loadRequests = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const url =
        from === "all"
          ? `${API_BASE}/api/admin/service-requests`
          : `${API_BASE}/api/admin/service-requests?from=${from}`;

      const res = await fetch(url, {
        method: "GET",
        credentials: "include",
      });

      if (res.status === 401) {
        setRequests([]);
        setError("Session expired. Please login again.");
        return;
      }

      if (!res.ok) {
        const msg = await res.text().catch(() => "Failed to load requests");
        throw new Error(msg);
      }

      const data = await res.json();
      setRequests(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Error loading requests:", err);
      setRequests([]);
      setError(err.message || "Failed to load requests");
    } finally {
      setLoading(false);
    }
  }, [from]);

  const markComplete = async (id) => {
    try {
      const res = await fetch(`${API_BASE}/api/admin/service-requests/${id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ status: "completed" }),
      });

      if (res.status === 401) {
        setError("Session expired. Please login again.");
        return;
      }

      if (!res.ok) {
        const msg = await res.text().catch(() => "Failed to update request");
        throw new Error(msg);
      }

      await loadRequests();
      navigate(`/admin/requests?view=${id}`);
    } catch (err) {
      console.error("Error updating status:", err);
      alert("Error updating status: " + (err.message || "Unknown error"));
    }
  };

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

  const safeRequests = Array.isArray(requests) ? requests : [];

  const prettyFrom = (r) => {
    const v = (r?.requestedFrom || "").toLowerCase();
    if (v === "android") return "Android App";
    return "Website";
  };

  const prettyRole = (r) => {
    const v = (r?.createdByRole || "").toLowerCase();
    if (!v) return "User";
    return v.charAt(0).toUpperCase() + v.slice(1);
  };

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

  const filteredRequests = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return safeRequests;

    return safeRequests.filter((req) => {
      const userName = String(req?.userName || "").toLowerCase();
      const service = String(req?.service || "").toLowerCase();
      const status = String(req?.status || "").toLowerCase();
      const providerType = String(req?.providerType || "").toLowerCase();
      const role = String(req?.createdByRole || "").toLowerCase();

      return (
        userName.includes(q) ||
        service.includes(q) ||
        status.includes(q) ||
        providerType.includes(q) ||
        role.includes(q)
      );
    });
  }, [safeRequests, search]);

  const selected = viewId
    ? filteredRequests.find((r) => String(r._id) === String(viewId)) ||
      safeRequests.find((r) => String(r._id) === String(viewId))
    : null;

  const stats = useMemo(() => {
    let website = 0;
    let android = 0;
    let pending = 0;
    let active = 0;
    let completed = 0;
    let cancelled = 0;

    for (const req of filteredRequests) {
      const src = String(req?.requestedFrom || "").toLowerCase();
      const st = statusClass(req?.status);

      if (src === "android") android += 1;
      else website += 1;

      if (st === "pending" || st === "assigned") pending += 1;
      else if (st === "active") active += 1;
      else if (st === "completed") completed += 1;
      else if (st === "cancelled") cancelled += 1;
    }

    return {
      total: filteredRequests.length,
      website,
      android,
      pending,
      active,
      completed,
      cancelled,
    };
  }, [filteredRequests]);

  return (
    <div className="requests-page">
      <main className="requests-container">
        <section className="requests-hero card-ui">
          <div>
            <div className="hero-badge">Admin / Requests</div>
            <h1>Service Requests</h1>
            <p>
              Review submitted requests, inspect details, filter by source, and
              update request completion status.
            </p>
          </div>

          <div className="hero-actions">
            <button
              type="button"
              className="btn btn-light"
              onClick={() => navigate("/admin")}
            >
              Back to Dashboard
            </button>
            <button type="button" className="btn btn-primary" onClick={loadRequests}>
              Refresh
            </button>
          </div>
        </section>

        <section className="requests-stats">
          <div className="stat-box">
            <span>Total Requests</span>
            <strong>{stats.total}</strong>
          </div>
          <div className="stat-box">
            <span>Website</span>
            <strong>{stats.website}</strong>
          </div>
          <div className="stat-box">
            <span>Android App</span>
            <strong>{stats.android}</strong>
          </div>
          <div className="stat-box">
            <span>Pending</span>
            <strong>{stats.pending}</strong>
          </div>
          <div className="stat-box">
            <span>Active</span>
            <strong>{stats.active}</strong>
          </div>
          <div className="stat-box">
            <span>Completed</span>
            <strong>{stats.completed}</strong>
          </div>
        </section>

        <section className="card-ui requests-toolbar">
          <div className="toolbar-group">
            <h3>Request Source</h3>
            <div className="toolbar-buttons">
              <button
                type="button"
                className={from === "all" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => setFrom("all")}
              >
                All
              </button>
              <button
                type="button"
                className={from === "web" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => setFrom("web")}
              >
                Website
              </button>
              <button
                type="button"
                className={from === "android" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => setFrom("android")}
              >
                Android App
              </button>
            </div>
          </div>

          <div className="toolbar-group">
            <h3>Search Requests</h3>
            <div className="search-row">
              <input
                className="search-input"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search by user, service, provider type, role or status..."
              />
              <button
                type="button"
                className="btn btn-light"
                onClick={() => setSearch("")}
              >
                Clear
              </button>
            </div>
          </div>
        </section>

        {error && <div className="error-box">{error}</div>}

        {selected && (
          <section className="card-ui request-details-card">
            <div className="details-head">
              <div>
                <h3>Request Details</h3>
                <p>Detailed information for the selected request.</p>
              </div>

              <button
                type="button"
                className="btn btn-light"
                onClick={() => navigate("/admin/requests")}
              >
                Close
              </button>
            </div>

            <div className="details-grid">
              <div className="detail-box">
                <span>User</span>
                <strong>{selected.userName || "—"}</strong>
              </div>

              <div className="detail-box">
                <span>Service</span>
                <strong>{selected.service || "—"}</strong>
              </div>

              <div className="detail-box">
                <span>Provider Type</span>
                <strong>{selected.providerType || "—"}</strong>
              </div>

              <div className="detail-box">
                <span>Status</span>
                <strong>
                  <span className={`pill ${statusClass(selected.status)}`}>
                    {prettyStatus(selected.status)}
                  </span>
                </strong>
              </div>

              <div className="detail-box">
                <span>From</span>
                <strong>{prettyFrom(selected)}</strong>
              </div>

              <div className="detail-box">
                <span>Created By</span>
                <strong>{prettyRole(selected)}</strong>
              </div>

              <div className="detail-box">
                <span>Requested On</span>
                <strong>
                  {selected.createdAt
                    ? new Date(selected.createdAt).toLocaleString()
                    : "—"}
                </strong>
              </div>

              <div className="detail-box">
                <span>Request ID</span>
                <strong>{selected._id || "—"}</strong>
              </div>

              {selected.description && (
                <div className="detail-box full">
                  <span>Description</span>
                  <strong>{selected.description}</strong>
                </div>
              )}

              {selected.location && (
                <div className="detail-box full">
                  <span>Location</span>
                  <strong>
                    {typeof selected.location === "string"
                      ? selected.location
                      : `${selected.location.lat || "—"}, ${
                          selected.location.lng || "—"
                        }`}
                  </strong>
                </div>
              )}
            </div>

            <div className="details-actions">
              {String(selected.status).toLowerCase() !== "completed" && (
                <button
                  type="button"
                  className="btn btn-success"
                  onClick={() => markComplete(selected._id)}
                >
                  Mark Complete
                </button>
              )}

              <button
                type="button"
                className="btn btn-light"
                onClick={loadRequests}
              >
                Refresh Details
              </button>
            </div>
          </section>
        )}

        <section className="card-ui requests-list-card">
          <div className="list-header">
            <h3>
              Showing {filteredRequests.length} request
              {filteredRequests.length === 1 ? "" : "s"}
            </h3>
          </div>

          {loading ? (
            <div className="empty-box">Loading requests...</div>
          ) : filteredRequests.length === 0 ? (
            <div className="empty-box">No requests found</div>
          ) : (
            <div className="request-list">
              {filteredRequests.map((req) => (
                <div className="request-row" key={req._id}>
                  <div className="request-left">
                    <div className="request-avatar">🛠️</div>

                    <div className="request-details">
                      <strong>{req.service || "—"}</strong>
                      <span>{req.userName || "Unnamed User"}</span>
                      <span>{req.providerType || "No provider type"}</span>
                    </div>
                  </div>

                  <div className="request-middle">
                    <span className={`pill ${statusClass(req.status)}`}>
                      {prettyStatus(req.status)}
                    </span>

                    <span className="pill web-app">{prettyFrom(req)}</span>

                    <span className="pill role-pill">{prettyRole(req)}</span>

                    <span className="date-pill">
                      {req.createdAt
                        ? new Date(req.createdAt).toLocaleString()
                        : "—"}
                    </span>
                  </div>

                  <div className="request-right">
                    <button
                      type="button"
                      className="btn btn-primary"
                      onClick={() => navigate(`/admin/requests?view=${req._id}`)}
                    >
                      View
                    </button>

                    {String(req.status).toLowerCase() !== "completed" && (
                      <button
                        type="button"
                        className="btn btn-success"
                        onClick={() => markComplete(req._id)}
                      >
                        Mark Complete
                      </button>
                    )}
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