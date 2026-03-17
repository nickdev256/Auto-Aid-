// src/pages/Admin/AdminUsers.jsx
import React, { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import AdminSidebar from "../../components/AdminSidebar";
import { getAdminUsers } from "../../services/api";
import "./AdminUsers.css";

export default function AdminUsers() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [from, setFrom] = useState(searchParams.get("from") || "all");
  const [q, setQ] = useState(searchParams.get("q") || "");

  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);

  const [lastUpdated, setLastUpdated] = useState(null);
  const abortRef = useRef(null);

  const pretty = (v) =>
    String(v || "").toLowerCase() === "android" ? "Android App" : "Website";

  // ✅ Debounced query so filtering doesn't lag
  const [debouncedQ, setDebouncedQ] = useState(q);
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQ(q), 200);
    return () => clearTimeout(t);
  }, [q]);

  const syncUrl = (nextFrom, nextQ) => {
    const next = {};
    if (nextFrom && nextFrom !== "all") next.from = nextFrom;
    if (nextQ) next.q = nextQ;
    setSearchParams(next, { replace: true });
  };

  const load = async () => {
    // cancel any previous request
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
  };

  // ✅ Load when filter changes
  useEffect(() => {
    syncUrl(from, q);
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [from]);

  // ✅ Keep URL synced when q changes too (without reloading server)
  useEffect(() => {
    syncUrl(from, q);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q]);

  // ✅ Close modal on ESC
  useEffect(() => {
    const onKeyDown = (e) => {
      if (e.key === "Escape") setSelected(null);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  // ✅ Client-side filtering
  const filtered = useMemo(() => {
    const query = (debouncedQ || "").trim().toLowerCase();
    if (!query) return users;

    return users.filter((u) => {
      const name = (u?.name || "").toLowerCase();
      const email = (u?.email || "").toLowerCase();
      const phone = (u?.phone || "").toLowerCase();
      const role = (u?.role || "").toLowerCase();
      return (
        name.includes(query) ||
        email.includes(query) ||
        phone.includes(query) ||
        role.includes(query)
      );
    });
  }, [users, debouncedQ]);

  return (
    <div className="admin-users-root">
      <AdminSidebar />

      <main className="admin-users-container" role="main">
        <div className="top-row">
          <div className="page-title">
            <h1>Users</h1>
            <p className="subtitle">
              See who registered via Website vs Android App
              {lastUpdated ? (
                <span className="muted-inline">
                  {" • "}Updated {lastUpdated.toLocaleTimeString()}
                </span>
              ) : null}
            </p>
          </div>

          <div className="top-actions">
            <button className="ghost-btn" onClick={load} type="button">
              Refresh
            </button>
            <button
              className="outline-btn"
              onClick={() => navigate("/admin")}
              type="button"
            >
              Back to Dashboard
            </button>
          </div>
        </div>

        <section className="users-toolbar card-acrylic">
          <div className="pill-row">
            <button
              className={from === "all" ? "outline-btn" : "ghost-btn"}
              onClick={() => setFrom("all")}
              type="button"
            >
              All
            </button>
            <button
              className={from === "web" ? "outline-btn" : "ghost-btn"}
              onClick={() => setFrom("web")}
              type="button"
            >
              Website
            </button>
            <button
              className={from === "android" ? "outline-btn" : "ghost-btn"}
              onClick={() => setFrom("android")}
              type="button"
            >
              Android App
            </button>
          </div>

          <div className="search-row">
            <input
              className="search-input"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Search by name, email, phone, role..."
            />
            <button className="ghost-btn" onClick={() => setQ("")} type="button">
              Clear
            </button>
          </div>
        </section>

        <section className="users-list card-acrylic">
          <div className="list-header">
            <h3>
              Showing {filtered.length} user{filtered.length === 1 ? "" : "s"}
            </h3>
          </div>

          {loading ? (
            <div className="empty">Loading users...</div>
          ) : filtered.length === 0 ? (
            <div className="empty">No users found</div>
          ) : (
            <ul className="list">
              {filtered.map((u) => (
                <li key={u._id} className="row">
                  <div className="left">
                    <strong>{u.name || "Unnamed user"}</strong>
                    <div className="small">{u.email || "No email"}</div>
                    <div className="small">{u.phone || "No phone"}</div>
                  </div>

                  <div className="right">
                    <span
                      className={`badge ${
                        String(u.registeredFrom).toLowerCase() === "android"
                          ? "app"
                          : "web"
                      }`}
                      title="Where the user first registered"
                    >
                      <span className="badge-label">Registered:</span>
                      <span className="badge-value">{pretty(u.registeredFrom)}</span>
                    </span>

                    <span
                      className={`badge ${
                        String(u.lastLoginFrom).toLowerCase() === "android"
                          ? "app"
                          : "web"
                      }`}
                      title="Where the user last logged in"
                    >
                      <span className="badge-label">Last login:</span>
                      <span className="badge-value">{pretty(u.lastLoginFrom)}</span>
                    </span>

                    <button
                      className="view-btn"
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
            <div className="modal" onClick={(e) => e.stopPropagation()}>
              <div className="modal-head">
                <h3>User Details</h3>
                <button
                  className="ghost-btn"
                  onClick={() => setSelected(null)}
                  type="button"
                >
                  Close
                </button>
              </div>

              <div className="modal-body">
                <div className="kv">
                  <span>Name</span>
                  <strong>{selected.name || "—"}</strong>
                </div>
                <div className="kv">
                  <span>Email</span>
                  <strong>{selected.email || "—"}</strong>
                </div>
                <div className="kv">
                  <span>Phone</span>
                  <strong>{selected.phone || "—"}</strong>
                </div>
                <div className="kv">
                  <span>Role</span>
                  <strong>{selected.role || "—"}</strong>
                </div>
                <div className="kv">
                  <span>Status</span>
                  <strong>{selected.status || "—"}</strong>
                </div>

                <div className="kv">
                  <span>Registered From</span>
                  <strong>{pretty(selected.registeredFrom)}</strong>
                </div>

                <div className="kv">
                  <span>Last Login From</span>
                  <strong>{pretty(selected.lastLoginFrom)}</strong>
                </div>

                <div className="kv">
                  <span>Created</span>
                  <strong>
                    {selected.createdAt
                      ? new Date(selected.createdAt).toLocaleString()
                      : "—"}
                  </strong>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}