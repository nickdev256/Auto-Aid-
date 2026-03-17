import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { approveProvider, activateSubscription } from "../../services/api";
import "./ProviderManagement.css";

export default function ProviderManagement() {
  const navigate = useNavigate();

  const [providers, setProviders] = useState([]);
  const [loading, setLoading] = useState(true);

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [subscribedFilter, setSubscribedFilter] = useState("");
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);

  const [showModal, setShowModal] = useState(false);
  const [selectedProvider, setSelectedProvider] = useState(null);
  const [actionProcessing, setActionProcessing] = useState(false);

  // ----------------------------------------
  // LOAD PROVIDERS
  // ----------------------------------------
  const loadProviders = async () => {
    setLoading(true);
    try {
      const query = new URLSearchParams({
        page,
        limit: pageSize,
        search,
        status: statusFilter,
        subscribed: subscribedFilter,
      });

     const res = await fetch(
  `http://localhost:5001/api/admin/providers?${query.toString()}`,
  { credentials: "include" } // ✅ send cookie
);
      

      if (!res.ok) throw new Error("Failed to load providers");

      const json = await res.json();
      setProviders(json.data || []);
      setTotalPages(json.pages || 1);
    } catch (err) {
      console.error("Load providers error:", err);
      setProviders([]);
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProviders();
  }, [page, search, statusFilter, subscribedFilter]);

  // ----------------------------------------
  // STATS HEADER
  // ----------------------------------------
  const stats = useMemo(() => {
    return {
      total: providers.length,
      pending: providers.filter((p) => p.status === "pending").length,
      approved: providers.filter((p) => p.status === "approved").length,
      subscribed: providers.filter((p) => p.subscriptionActive).length,
    };
  }, [providers]);

  // ----------------------------------------
  // VIEW PROFILE (MODAL)
  // ----------------------------------------
  const viewProfile = async (id) => {
    setLoading(true);
    try {
      const res = await fetch(`http://localhost:5001/api/admin/providers/${id}`, {
  credentials: "include", // ✅ send cookie
});
      if (!res.ok) throw new Error("Failed to load profile");

      const data = await res.json();
      data.id = data.id || data._id; // normalize
      setSelectedProvider(data);
      setShowModal(true);
    } catch (err) {
      console.error(err);
      alert("Failed to load provider profile.");
    } finally {
      setLoading(false);
    }
  };

  // ----------------------------------------
  // APPROVE PROVIDER
  // ----------------------------------------
  const handleApprove = async (id) => {
    if (!window.confirm("Approve this provider?")) return;

    setActionProcessing(true);
    try {
      await approveProvider(id);
      await loadProviders();
      alert("Provider approved successfully.");
    } catch (err) {
      console.error("Approve error:", err);
      alert("Failed to approve provider.");
    } finally {
      setActionProcessing(false);
    }
  };

  // ----------------------------------------
  // ACTIVATE SUBSCRIPTION
  // ----------------------------------------
  const handleActivateSubscription = async (id) => {
    if (!window.confirm("Activate subscription?")) return;

    setActionProcessing(true);
    try {
      await activateSubscription(id, "monthly");
      await loadProviders();
      alert("Subscription activated.");
    } catch (err) {
      console.error("Activate subscription error:", err);
      alert("Failed to activate subscription.");
    } finally {
      setActionProcessing(false);
    }
  };

  // ----------------------------------------
  // MODAL ACTIONS
  // ----------------------------------------
  const approveFromModal = async () => {
    if (!selectedProvider) return;
    await handleApprove(selectedProvider.id); // FIXED
    setShowModal(false);
  };

  const activateFromModal = async () => {
    if (!selectedProvider) return;
    await handleActivateSubscription(selectedProvider.id); // FIXED
    setShowModal(false);
  };

  // ----------------------------------------
  // LOADING SCREEN
  // ----------------------------------------
  if (loading) {
    return (
      <div className="provider-management-container">
        <div className="top-row">
          <button className="admin-back" onClick={() => navigate(-1)}>
            ← Back
          </button>
          <div className="page-title">
            <h2>Provider Management</h2>
            <p className="subtitle">Loading providers…</p>
          </div>
        </div>
      </div>
    );
  }

  // ----------------------------------------
  // MAIN RENDER
  // ----------------------------------------
  return (
    <div className="provider-management-container">

      {/* HEADER */}
      <div className="top-row">
        <button className="admin-back" onClick={() => navigate(-1)}>
          ← Back
        </button>

        <div className="page-title">
          <h2>Provider Management</h2>
          <p className="subtitle">
            Manage all providers, approvals and subscription activations
          </p>
        </div>

        <div className="stats-row">
          <div className="stat-card">
            <div className="stat-num">{stats.total}</div>
            <div className="stat-label">Total</div>
          </div>

          <div className="stat-card pending">
            <div className="stat-num">{stats.pending}</div>
            <div className="stat-label">Pending</div>
          </div>

          <div className="stat-card approved">
            <div className="stat-num">{stats.approved}</div>
            <div className="stat-label">Approved</div>
          </div>

          <div className="stat-card subscribed">
            <div className="stat-num">{stats.subscribed}</div>
            <div className="stat-label">Subscribed</div>
          </div>
        </div>
      </div>

      {/* FILTERS */}
      <div className="filters premium-filters">
        <input
          type="text"
          placeholder="Search by name or email…"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(1);
          }}
        />

        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setPage(1);
          }}
        >
          <option value="">All Status</option>
          <option value="pending">Pending</option>
          <option value="approved">Approved</option>
          <option value="inactive">Inactive</option>
        </select>

        <select
          value={subscribedFilter}
          onChange={(e) => {
            setSubscribedFilter(e.target.value);
            setPage(1);
          }}
        >
          <option value="">Any Subscription</option>
          <option value="true">Subscribed</option>
          <option value="false">Not Subscribed</option>
        </select>

        <button
          className="btn small primary"
          style={{ marginLeft: "auto" }}
          onClick={() => {
            setSearch("");
            setStatusFilter("");
            setSubscribedFilter("");
          }}
        >
          Reset
        </button>
      </div>

      {/* TABLE */}
      <div className="table-wrap">
        <table className="provider-table premium">
          <thead>
            <tr>
              <th>Provider</th>
              <th>Phone</th>
              <th>Status</th>
              <th>Subscription</th>
              <th>Expiry</th>
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>
            {providers.length === 0 ? (
              <tr>
                <td colSpan={6} className="empty-row">
                  No providers found
                </td>
              </tr>
            ) : (
              providers.map((p) => {
                const id = p.id; // 🔥 FIXED — ALWAYS USE p.id

                return (
                  <tr key={id}>
                    <td className="provider-col">
                      <div className="avatar">{p.name?.charAt(0)}</div>
                      <div>
                        <div className="name">{p.name}</div>
                        <div className="email">{p.email}</div>
                      </div>
                    </td>

                    <td>{p.phone || "—"}</td>

                    <td>
                      <span className={`badge ${p.status}`}>
                        {p.status.toUpperCase()}
                      </span>
                    </td>

                    <td>
                      {p.subscriptionActive ? (
                        <span className="badge active-sub">ACTIVE</span>
                      ) : (
                        <span className="badge no-sub">NO SUBSCRIPTION</span>
                      )}
                    </td>

                    <td>
                      {p.subscriptionExpiry
                        ? new Date(p.subscriptionExpiry).toLocaleDateString()
                        : "—"}
                    </td>

                    <td className="action-buttons">
                      <button
                        className="btn small view"
                        onClick={() => viewProfile(id)}
                      >
                        View
                      </button>

                      {p.status === "pending" && (
                        <button
                          className="btn small approve"
                          onClick={() => handleApprove(id)}
                          disabled={actionProcessing}
                        >
                          Approve
                        </button>
                      )}

                      {!p.subscriptionActive && p.status === "approved" && (
                        <button
                          className="btn small subscribe"
                          onClick={() => handleActivateSubscription(id)}
                          disabled={actionProcessing}
                        >
                          Activate
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* PAGINATION */}
      <div className="pagination premium-pager">
        <button disabled={page <= 1} onClick={() => setPage(page - 1)}>
          Prev
        </button>

        <span>
          Page {page} of {totalPages}
        </span>

        <button disabled={page >= totalPages} onClick={() => setPage(page + 1)}>
          Next
        </button>
      </div>

      {/* MODAL */}
      {showModal && selectedProvider && (
        <div
          className="modal-backdrop"
          onClick={() => {
            setShowModal(false);
            setSelectedProvider(null);
          }}
        >
          <div className="modal premium-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{selectedProvider.name}</h3>
              <div className="meta">
                {selectedProvider.businessType || "Provider"}
              </div>
            </div>

            <div className="modal-body">
              <p><strong>Email:</strong> {selectedProvider.email}</p>
              <p><strong>Phone:</strong> {selectedProvider.phone || "—"}</p>
              <p><strong>Address:</strong> {selectedProvider.address || "—"}</p>
              <p>
                <strong>Status:</strong>{" "}
                <span className={`badge ${selectedProvider.status}`}>
                  {selectedProvider.status}
                </span>
              </p>

              <div className="sub-info">
                <p>
                  <strong>Subscription:</strong>{" "}
                  {selectedProvider.subscription?.active
                    ? selectedProvider.subscription.plan
                    : "Not Subscribed"}
                </p>
                <p>
                  <strong>Expiry:</strong>{" "}
                  {selectedProvider.subscription?.expiryDate
                    ? new Date(
                        selectedProvider.subscription.expiryDate
                      ).toLocaleDateString()
                    : "—"}
                </p>
              </div>
            </div>

            <div className="modal-actions">
              {selectedProvider.status === "pending" && (
                <button
                  className="btn approve"
                  onClick={approveFromModal}
                  disabled={actionProcessing}
                >
                  Approve
                </button>
              )}

              {!selectedProvider.subscription?.active &&
                selectedProvider.status === "approved" && (
                  <button
                    className="btn subscribe"
                    onClick={activateFromModal}
                    disabled={actionProcessing}
                  >
                    Activate Subscription
                  </button>
                )}

              <button className="btn ghost" onClick={() => setShowModal(false)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
