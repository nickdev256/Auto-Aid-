import React, { useEffect, useMemo, useRef, useState, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  getAdminUsers,
  approveVerification,
  rejectVerification,
} from "../../services/api";
import "./AdminUsers.css";

const API_BASE =
  import.meta.env.VITE_API_URL?.replace(/\/$/, "") || "http://localhost:5001";

export default function AdminUsers() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [from, setFrom] = useState(searchParams.get("from") || "all");
  const [q, setQ] = useState(searchParams.get("q") || "");
  const [verificationFilter, setVerificationFilter] = useState(
    searchParams.get("verification") || "all"
  );

  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(null);

  const abortRef = useRef(null);

  const pretty = (v) =>
    String(v || "").toLowerCase() === "android" ? "Android App" : "Website";

  const prettyVerification = (v) => {
    const value = String(v || "not_verified").toLowerCase();
    if (value === "verified") return "Verified";
    if (value === "pending") return "Pending Review";
    if (value === "rejected") return "Rejected";
    return "Not Verified";
  };

  const verificationClass = (v) => {
    const value = String(v || "not_verified").toLowerCase();
    if (value === "verified") return "verified";
    if (value === "pending") return "pending";
    if (value === "rejected") return "rejected";
    return "not-verified";
  };

  const buildDocumentUrl = (docPath) => {
    if (!docPath) return "";
    if (docPath.startsWith("http://") || docPath.startsWith("https://")) {
      return docPath;
    }
    return `${API_BASE}/${String(docPath).replace(/^\/+/, "")}`;
  };

  const [debouncedQ, setDebouncedQ] = useState(q);

  useEffect(() => {
    const t = setTimeout(() => setDebouncedQ(q), 200);
    return () => clearTimeout(t);
  }, [q]);

  const syncUrl = useCallback(
    (nextFrom, nextQ, nextVerification) => {
      const next = {};
      if (nextFrom && nextFrom !== "all") next.from = nextFrom;
      if (nextQ) next.q = nextQ;
      if (nextVerification && nextVerification !== "all") {
        next.verification = nextVerification;
      }
      setSearchParams(next, { replace: true });
    },
    [setSearchParams]
  );

  const load = useCallback(async () => {
    if (abortRef.current) abortRef.current.abort();

    const controller = new AbortController();
    abortRef.current = controller;

    setLoading(true);
    try {
      const data = await getAdminUsers(from, { signal: controller.signal });
      setUsers(Array.isArray(data) ? data : []);
      setLastUpdated(new Date());
    } catch (e) {
      if (e?.name !== "AbortError") {
        console.error("Load users error:", e);
        setUsers([]);
      }
    } finally {
      setLoading(false);
    }
  }, [from]);

  useEffect(() => {
    syncUrl(from, q, verificationFilter);
    load();
  }, [from, load, q, verificationFilter, syncUrl]);

  useEffect(() => {
    const onKeyDown = (e) => {
      if (e.key === "Escape") setSelected(null);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  const filtered = useMemo(() => {
    const query = (debouncedQ || "").trim().toLowerCase();

    return users.filter((u) => {
      const name = (u?.name || "").toLowerCase();
      const email = (u?.email || "").toLowerCase();
      const phone = (u?.phone || "").toLowerCase();
      const role = (u?.role || "").toLowerCase();
      const verificationStatus = String(
        u?.verificationStatus || "not_verified"
      ).toLowerCase();

      const matchesQuery =
        !query ||
        name.includes(query) ||
        email.includes(query) ||
        phone.includes(query) ||
        role.includes(query) ||
        verificationStatus.includes(query);

      const matchesVerification =
        verificationFilter === "all" ||
        verificationStatus === verificationFilter;

      return matchesQuery && matchesVerification;
    });
  }, [users, debouncedQ, verificationFilter]);

  const stats = useMemo(() => {
    let website = 0;
    let android = 0;
    let verified = 0;
    let pending = 0;
    let rejected = 0;
    let notVerified = 0;

    for (const user of filtered) {
      const registeredFrom = String(user?.registeredFrom || "").toLowerCase();
      const verificationStatus = String(
        user?.verificationStatus || "not_verified"
      ).toLowerCase();

      if (registeredFrom === "android") android += 1;
      else website += 1;

      if (verificationStatus === "verified") verified += 1;
      else if (verificationStatus === "pending") pending += 1;
      else if (verificationStatus === "rejected") rejected += 1;
      else notVerified += 1;
    }

    return {
      total: filtered.length,
      website,
      android,
      verified,
      pending,
      rejected,
      notVerified,
    };
  }, [filtered]);

  const updateSelectedAndList = (updatedUser) => {
    setUsers((prev) =>
      prev.map((u) =>
        String(u._id || u.id) === String(updatedUser._id || updatedUser.id)
          ? { ...u, ...updatedUser }
          : u
      )
    );
    setSelected(updatedUser);
  };

  const handleApprove = async (id) => {
    setActionLoading(true);
    try {
      const res = await approveVerification(id);
      if (res?.user) updateSelectedAndList(res.user);
      alert(res?.message || "Verification approved");
      await load();
      setSelected(null);
    } catch (e) {
      console.error("Approve verification error:", e);
      alert(e?.message || "Failed to approve verification");
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async (id) => {
    const reason = window.prompt("Enter rejection reason (optional):", "") || "";
    setActionLoading(true);
    try {
      const res = await rejectVerification(id, reason);
      if (res?.user) updateSelectedAndList(res.user);
      alert(res?.message || "Verification rejected");
      await load();
      setSelected(null);
    } catch (e) {
      console.error("Reject verification error:", e);
      alert(e?.message || "Failed to reject verification");
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="admin-users-page">
      <main className="admin-users-container">
        <section className="users-hero card-ui">
          <div>
            <div className="hero-badge">Admin / Users</div>
            <h1>Users Management</h1>
            <p>
              Manage users, review verification documents, and monitor where
              registrations come from.
              {lastUpdated ? (
                <span className="muted-inline">
                  {" "}• Updated {lastUpdated.toLocaleTimeString()}
                </span>
              ) : null}
            </p>
          </div>

          <div className="hero-actions">
            <button className="btn btn-light" onClick={load} type="button">
              Refresh
            </button>
            <button
              className="btn btn-primary"
              onClick={() => navigate("/admin")}
              type="button"
            >
              Back to Dashboard
            </button>
          </div>
        </section>

        <section className="users-stats">
          <div className="stat-box">
            <span>Total Users</span>
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
            <span>Verified</span>
            <strong>{stats.verified}</strong>
          </div>
          <div className="stat-box">
            <span>Pending Review</span>
            <strong>{stats.pending}</strong>
          </div>
          <div className="stat-box">
            <span>Rejected</span>
            <strong>{stats.rejected}</strong>
          </div>
        </section>

        <section className="card-ui users-toolbar">
          <div className="toolbar-group">
            <h3>Registration Source</h3>
            <div className="toolbar-buttons">
              <button
                className={from === "all" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => setFrom("all")}
                type="button"
              >
                All
              </button>
              <button
                className={from === "web" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => setFrom("web")}
                type="button"
              >
                Website
              </button>
              <button
                className={from === "android" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => setFrom("android")}
                type="button"
              >
                Android App
              </button>
            </div>
          </div>

          <div className="toolbar-group">
            <h3>Verification Filter</h3>
            <div className="toolbar-buttons">
              <button
                className={verificationFilter === "all" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => setVerificationFilter("all")}
                type="button"
              >
                All Verification
              </button>
              <button
                className={verificationFilter === "pending" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => setVerificationFilter("pending")}
                type="button"
              >
                Pending
              </button>
              <button
                className={verificationFilter === "verified" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => setVerificationFilter("verified")}
                type="button"
              >
                Verified
              </button>
              <button
                className={verificationFilter === "rejected" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => setVerificationFilter("rejected")}
                type="button"
              >
                Rejected
              </button>
              <button
                className={verificationFilter === "not_verified" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => setVerificationFilter("not_verified")}
                type="button"
              >
                Not Verified
              </button>
            </div>
          </div>

          <div className="toolbar-group">
            <h3>Search Users</h3>
            <div className="search-row">
              <input
                className="search-input"
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Search by name, email, phone, role, verification..."
              />
              <button className="btn btn-light" onClick={() => setQ("")} type="button">
                Clear
              </button>
            </div>
          </div>
        </section>

        <section className="card-ui users-list-card">
          <div className="list-header">
            <h3>
              Showing {filtered.length} user{filtered.length === 1 ? "" : "s"}
            </h3>
          </div>

          {loading ? (
            <div className="empty-box">Loading users...</div>
          ) : filtered.length === 0 ? (
            <div className="empty-box">No users found</div>
          ) : (
            <ul className="users-list">
              {filtered.map((u) => (
                <li key={u._id || u.id} className="user-row">
                  <div className="user-left">
                    <div className="user-avatar">
                      {(u?.name || "U").charAt(0).toUpperCase()}
                    </div>

                    <div className="user-details">
                      <strong>{u.name || "Unnamed user"}</strong>
                      <span>{u.email || "No email"}</span>
                      <span>{u.phone || "No phone"}</span>
                    </div>
                  </div>

                  <div className="user-right">
                    <span
                      className={`pill ${
                        String(u.registeredFrom).toLowerCase() === "android"
                          ? "app"
                          : "web"
                      }`}
                    >
                      Registered: {pretty(u.registeredFrom)}
                    </span>

                    <span
                      className={`pill ${
                        String(u.lastLoginFrom).toLowerCase() === "android"
                          ? "app"
                          : "web"
                      }`}
                    >
                      Last login: {pretty(u.lastLoginFrom)}
                    </span>

                    <span
                      className={`pill verification ${verificationClass(
                        u.verificationStatus
                      )}`}
                    >
                      {prettyVerification(u.verificationStatus)}
                    </span>

                    <button
                      className="btn btn-primary"
                      onClick={() => setSelected(u)}
                      type="button"
                    >
                      View
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        {selected && (
          <div className="modal-overlay" onClick={() => setSelected(null)}>
            <div className="modal-card" onClick={(e) => e.stopPropagation()}>
              <div className="modal-head">
                <h3>User Details</h3>
                <button
                  className="btn btn-light"
                  onClick={() => setSelected(null)}
                  type="button"
                >
                  Close
                </button>
              </div>

              <div className="modal-body">
                <div className="detail-box">
                  <span>Name</span>
                  <strong>{selected.name || "—"}</strong>
                </div>
                <div className="detail-box">
                  <span>Email</span>
                  <strong>{selected.email || "—"}</strong>
                </div>
                <div className="detail-box">
                  <span>Phone</span>
                  <strong>{selected.phone || "—"}</strong>
                </div>
                <div className="detail-box">
                  <span>Role</span>
                  <strong>{selected.role || "—"}</strong>
                </div>
                <div className="detail-box">
                  <span>Status</span>
                  <strong>{selected.status || "—"}</strong>
                </div>
                <div className="detail-box">
                  <span>Verification Status</span>
                  <strong className={verificationClass(selected.verificationStatus)}>
                    {prettyVerification(selected.verificationStatus)}
                  </strong>
                </div>
                <div className="detail-box">
                  <span>Document Type</span>
                  <strong>{selected.verificationDocumentType || "—"}</strong>
                </div>
                <div className="detail-box">
                  <span>Registered From</span>
                  <strong>{pretty(selected.registeredFrom)}</strong>
                </div>
                <div className="detail-box">
                  <span>Last Login From</span>
                  <strong>{pretty(selected.lastLoginFrom)}</strong>
                </div>
                <div className="detail-box">
                  <span>Created</span>
                  <strong>
                    {selected.createdAt
                      ? new Date(selected.createdAt).toLocaleString()
                      : "—"}
                  </strong>
                </div>
                <div className="detail-box">
                  <span>Submitted At</span>
                  <strong>
                    {selected.verificationSubmittedAt
                      ? new Date(selected.verificationSubmittedAt).toLocaleString()
                      : "—"}
                  </strong>
                </div>
                <div className="detail-box">
                  <span>Reviewed At</span>
                  <strong>
                    {selected.verificationReviewedAt
                      ? new Date(selected.verificationReviewedAt).toLocaleString()
                      : "—"}
                  </strong>
                </div>
                <div className="detail-box">
                  <span>Rejection Reason</span>
                  <strong>{selected.verificationRejectionReason || "—"}</strong>
                </div>

                {selected.verificationDocumentUrl ? (
                  <div className="document-box">
                    <div className="document-head">
                      <span>Uploaded Document</span>
                      <a
                        href={buildDocumentUrl(selected.verificationDocumentUrl)}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Open Full Image
                      </a>
                    </div>

                    <img
                      src={buildDocumentUrl(selected.verificationDocumentUrl)}
                      alt="Verification document"
                      className="document-image"
                    />
                  </div>
                ) : (
                  <div className="empty-box modal-empty">No uploaded document</div>
                )}
              </div>

              <div className="modal-actions">
                <button
                  className="btn btn-success"
                  type="button"
                  disabled={
                    actionLoading ||
                    String(selected.verificationStatus).toLowerCase() === "verified"
                  }
                  onClick={() => handleApprove(selected._id || selected.id)}
                >
                  {actionLoading ? "Processing..." : "Approve"}
                </button>

                <button
                  className="btn btn-danger"
                  type="button"
                  disabled={actionLoading}
                  onClick={() => handleReject(selected._id || selected.id)}
                >
                  {actionLoading ? "Processing..." : "Reject"}
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}