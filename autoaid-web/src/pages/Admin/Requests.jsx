import React, { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import "./Requests.css";

export default function Requests() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [from, setFrom] = useState("all");

  const navigate = useNavigate();
  const location = useLocation();

  // ✅ reacts to URL changes like ?view=123
  const viewId = new URLSearchParams(location.search).get("view");

  const loadRequests = async () => {
    setLoading(true);
    setError("");

    try {
      const url =
        from === "all"
          ? "http://localhost:5001/api/admin/service-requests"
          : `http://localhost:5001/api/admin/service-requests?from=${from}`;

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
  };

  const markComplete = async (id) => {
    try {
      const res = await fetch(`http://localhost:5001/api/admin/service-requests/${id}`, {
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

      // keep details open after marking complete
      navigate(`/admin/requests?view=${id}`);
    } catch (err) {
      console.error("Error updating status:", err);
      alert("Error updating status: " + (err.message || "Unknown error"));
    }
  };

  useEffect(() => {
    loadRequests();
  }, [from]);

  const safeRequests = Array.isArray(requests) ? requests : [];

  const selected = viewId
    ? safeRequests.find((r) => String(r._id) === String(viewId))
    : null;

  const prettyFrom = (r) => {
    const v = (r?.requestedFrom || "").toLowerCase();
    if (v === "android") return "Android";
    return "Web";
  };

  const prettyRole = (r) => {
    const v = (r?.createdByRole || "").toLowerCase();
    if (!v) return "user";
    return v;
  };

  if (loading) return <p>Loading requests...</p>;

  return (
    <div className="requests-container">
      <button type="button" onClick={() => navigate(-1)} className="admin-back">
        ← Back
      </button>

      <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
        <h2 style={{ marginRight: "auto" }}>Requests</h2>

        <button
          type="button"
          className={from === "all" ? "outline-btn" : "ghost-btn"}
          onClick={() => setFrom("all")}
        >
          All
        </button>

        <button
          type="button"
          className={from === "web" ? "outline-btn" : "ghost-btn"}
          onClick={() => setFrom("web")}
        >
          Web
        </button>

        <button
          type="button"
          className={from === "android" ? "outline-btn" : "ghost-btn"}
          onClick={() => setFrom("android")}
        >
          App
        </button>
      </div>

      {error && (
        <div className="error-box" style={{ marginBottom: 16 }}>
          {error}
        </div>
      )}

      {selected && (
        <div className="single-request-box">
          <h3>Request Details</h3>

          <p><strong>User:</strong> {selected.userName || "—"}</p>
          <p><strong>Service:</strong> {selected.service || "—"}</p>
          <p><strong>Provider Type:</strong> {selected.providerType || "—"}</p>
          <p><strong>Status:</strong> {selected.status || "—"}</p>
          <p><strong>From:</strong> {prettyFrom(selected)}</p>
          <p><strong>Created By:</strong> {prettyRole(selected)}</p>
          <p>
            <strong>Requested On:</strong>{" "}
            {selected.createdAt ? new Date(selected.createdAt).toLocaleString() : "—"}
          </p>

          {selected.description && (
            <p><strong>Description:</strong> {selected.description}</p>
          )}

          {selected.location && (
            <p>
              <strong>Location:</strong>{" "}
              {typeof selected.location === "string"
                ? selected.location
                : `${selected.location.lat || "—"}, ${selected.location.lng || "—"}`}
            </p>
          )}

          <div className="action-buttons" style={{ marginTop: 14 }}>
            {selected.status !== "completed" && (
              <button
                type="button"
                className="btn-complete"
                onClick={() => markComplete(selected._id)}
              >
                Mark Complete
              </button>
            )}

            <button
              type="button"
              className="btn-view"
              onClick={() => navigate("/admin/requests")}
            >
              Close
            </button>
          </div>
        </div>
      )}

      <table className="requests-table">
        <thead>
          <tr>
            <th>User</th>
            <th>Service</th>
            <th>Status</th>
            <th>From</th>
            <th>Created By</th>
            <th>Requested On</th>
            <th>Action</th>
          </tr>
        </thead>

        <tbody>
          {safeRequests.length === 0 ? (
            <tr>
              <td colSpan={7} style={{ textAlign: "center", padding: 16 }}>
                No requests found
              </td>
            </tr>
          ) : (
            safeRequests.map((req) => (
              <tr key={req._id}>
                <td>{req.userName || "—"}</td>
                <td>{req.service || "—"}</td>
                <td>
                  <span className={`badge ${req.status || ""}`}>
                    {req.status || "—"}
                  </span>
                </td>
                <td>{prettyFrom(req)}</td>
                <td>{prettyRole(req)}</td>
                <td>
                  {req.createdAt ? new Date(req.createdAt).toLocaleString() : "—"}
                </td>
                <td>
                  <div className="action-buttons">
                    <button
                      type="button"
                      className="btn-view"
                      onClick={() => navigate(`/admin/requests?view=${req._id}`)}
                    >
                      View
                    </button>

                    {req.status !== "completed" && (
                      <button
                        type="button"
                        className="btn-complete"
                        onClick={() => markComplete(req._id)}
                      >
                        Mark Complete
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>

      <div style={{ marginTop: 16 }}>
        <button type="button" className="btn-view" onClick={loadRequests}>
          Refresh
        </button>
      </div>
    </div>
  );
}