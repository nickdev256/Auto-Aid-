import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
  getProviders,
  getProviderById,
  approveProvider,
  activateSubscription,
  approveProviderVerification,
  rejectProviderVerification,
} from "../../services/api";
import "./ProviderManagement.css";

export default function ProviderManagement() {
  const navigate = useNavigate();

  const [providers, setProviders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [verificationFilter, setVerificationFilter] = useState("");
  const [subscribedFilter, setSubscribedFilter] = useState("");

  const [page, setPage] = useState(1);
  const [limit] = useState(10);
  const [pages, setPages] = useState(1);

  const [selectedProvider, setSelectedProvider] = useState(null);
  const [showModal, setShowModal] = useState(false);

  const [showRejectBox, setShowRejectBox] = useState(false);
  const [rejectReason, setRejectReason] = useState("");

  const normalizeProvider = (provider) => {
    if (!provider) return null;

    const subscription =
      provider.subscription || {
        active: provider.subscriptionActive || false,
        expiryDate: provider.subscriptionExpiry || null,
        plan: provider.subscriptionPlan || "",
      };

    const verificationStatus =
      provider.providerVerification?.status ||
      provider.verificationStatus ||
      "not_verified";

    const rejectionReason =
      provider.providerVerification?.rejectionReason ||
      provider.verificationRejectionReason ||
      "";

    const licenseDocumentUrl =
      provider.providerVerification?.licenseDocumentUrl ||
      provider.workLicenseDocumentUrl ||
      "";

    const businessDocumentUrl =
      provider.providerVerification?.businessDocumentUrl ||
      provider.businessRegistrationDocumentUrl ||
      "";

    const profileImageUrl =
      provider.providerVerification?.profileImageUrl ||
      provider.profileImage ||
      "";

    const submittedAt =
      provider.providerVerification?.submittedAt ||
      provider.verificationSubmittedAt ||
      null;

    const reviewedAt =
      provider.providerVerification?.reviewedAt ||
      provider.verificationReviewedAt ||
      null;

    return {
      ...provider,
      id: provider.id || provider._id,
      name: provider.name || "",
      businessName: provider.businessName || "",
      email: provider.email || "",
      phone: provider.phone || "",
      providerType: provider.providerType || provider.businessType || "",
      businessType: provider.businessType || provider.providerType || "",
      address: provider.address || "",
      status: provider.status || "pending",
      isApprovedProvider: provider.isApprovedProvider === true,
      subscription,
      providerVerification: {
        status: verificationStatus,
        licenseDocumentUrl,
        businessDocumentUrl,
        profileImageUrl,
        rejectionReason,
        submittedAt,
        reviewedAt,
      },
    };
  };

  const extractProviders = (res) => {
    if (Array.isArray(res?.providers)) return res.providers;
    if (Array.isArray(res?.data?.providers)) return res.data.providers;
    if (Array.isArray(res?.data)) return res.data;
    if (Array.isArray(res)) return res;
    return [];
  };

  const extractPages = (res) => {
    return res?.pages || res?.data?.pages || 1;
  };

  const loadProviders = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getProviders({
        page,
        limit,
        search,
        status: statusFilter,
        subscribed: subscribedFilter,
        verificationStatus: verificationFilter,
      });

      const rows = extractProviders(res).map(normalizeProvider);

      setProviders(rows);
      setPages(extractPages(res));
    } catch (error) {
      console.error("Failed to load providers:", error);
      setProviders([]);
      setPages(1);
    } finally {
      setLoading(false);
    }
  }, [page, limit, search, statusFilter, verificationFilter, subscribedFilter]);

  useEffect(() => {
    loadProviders();
  }, [loadProviders]);

  const stats = useMemo(() => {
    return {
      total: providers.length,
      pendingAccounts: providers.filter((p) => p.status === "pending").length,
      approvedAccounts: providers.filter((p) => p.status === "approved").length,
      activeSubscriptions: providers.filter((p) => p.subscription?.active).length,
      pendingVerification: providers.filter(
        (p) => p.providerVerification?.status === "pending"
      ).length,
      verifiedProviders: providers.filter(
        (p) => p.providerVerification?.status === "verified"
      ).length,
    };
  }, [providers]);

  const openProvider = async (id) => {
    setActionLoading(true);
    try {
      const res = await getProviderById(id);
      const rawProvider =
        res?.provider || res?.data?.provider || res?.data || res;
      const provider = normalizeProvider(rawProvider);
      setSelectedProvider(provider);
      setShowModal(true);
      setShowRejectBox(false);
      setRejectReason("");
    } catch (error) {
      console.error("Failed to load provider details:", error);
      alert(error.message || "Failed to load provider details.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleApproveAccount = async (id) => {
    if (!window.confirm("Approve this provider account?")) return;

    setActionLoading(true);
    try {
      await approveProvider(id);
      await loadProviders();

      if (selectedProvider?.id === id) {
        await openProvider(id);
      }

      alert("Provider account approved successfully.");
    } catch (error) {
      console.error(error);
      alert(error.message || "Failed to approve provider account.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleActivateSubscription = async (id) => {
    if (!window.confirm("Activate subscription for this provider?")) return;

    setActionLoading(true);
    try {
      await activateSubscription(id, "monthly");
      await loadProviders();

      if (selectedProvider?.id === id) {
        await openProvider(id);
      }

      alert("Subscription activated successfully.");
    } catch (error) {
      console.error(error);
      alert(error.message || "Failed to activate subscription.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleApproveVerification = async (id) => {
    if (!window.confirm("Approve this provider verification?")) return;

    setActionLoading(true);
    try {
      await approveProviderVerification(id);
      await loadProviders();

      if (selectedProvider?.id === id) {
        await openProvider(id);
      }

      alert("Provider verification approved successfully.");
    } catch (error) {
      console.error(error);
      alert(error.message || "Failed to approve provider verification.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectVerification = async (id) => {
    if (!rejectReason.trim()) {
      alert("Please enter a rejection reason.");
      return;
    }

    setActionLoading(true);
    try {
      await rejectProviderVerification(id, rejectReason.trim());
      await loadProviders();

      if (selectedProvider?.id === id) {
        await openProvider(id);
      }

      setShowRejectBox(false);
      setRejectReason("");
      alert("Provider verification rejected successfully.");
    } catch (error) {
      console.error(error);
      alert(error.message || "Failed to reject provider verification.");
    } finally {
      setActionLoading(false);
    }
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedProvider(null);
    setShowRejectBox(false);
    setRejectReason("");
  };

  const renderVerificationBadge = (status) => {
    const safe = status || "not_verified";
    return (
      <span className={`pill verification ${safe}`}>
        {safe.replace(/_/g, " ").toUpperCase()}
      </span>
    );
  };

  const renderAccountBadge = (status) => {
    const safe = status || "pending";
    return <span className={`pill account ${safe}`}>{safe.toUpperCase()}</span>;
  };

  return (
    <div className="provider-page">
      <main className="provider-container">
        <section className="provider-hero card-ui">
          <div>
            <div className="hero-badge">Admin / Providers</div>
            <h1>Provider Management</h1>
            <p>
              Manage provider accounts, verification documents, subscriptions,
              and approval workflows in one clean view.
            </p>
          </div>

          <div className="hero-actions">
            <button className="btn btn-light" onClick={loadProviders} type="button">
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

        <section className="provider-stats">
          <div className="stat-box">
            <span>Total Providers</span>
            <strong>{stats.total}</strong>
          </div>
          <div className="stat-box">
            <span>Pending Accounts</span>
            <strong>{stats.pendingAccounts}</strong>
          </div>
          <div className="stat-box">
            <span>Approved Accounts</span>
            <strong>{stats.approvedAccounts}</strong>
          </div>
          <div className="stat-box">
            <span>Active Subscriptions</span>
            <strong>{stats.activeSubscriptions}</strong>
          </div>
          <div className="stat-box">
            <span>Verification Pending</span>
            <strong>{stats.pendingVerification}</strong>
          </div>
          <div className="stat-box">
            <span>Verified Providers</span>
            <strong>{stats.verifiedProviders}</strong>
          </div>
        </section>

        <section className="card-ui provider-toolbar">
          <div className="toolbar-group">
            <h3>Account Status</h3>
            <div className="toolbar-buttons">
              <button
                type="button"
                className={statusFilter === "" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => {
                  setStatusFilter("");
                  setPage(1);
                }}
              >
                All Status
              </button>
              <button
                type="button"
                className={statusFilter === "pending" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => {
                  setStatusFilter("pending");
                  setPage(1);
                }}
              >
                Pending
              </button>
              <button
                type="button"
                className={statusFilter === "approved" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => {
                  setStatusFilter("approved");
                  setPage(1);
                }}
              >
                Approved
              </button>
              <button
                type="button"
                className={statusFilter === "inactive" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => {
                  setStatusFilter("inactive");
                  setPage(1);
                }}
              >
                Inactive
              </button>
            </div>
          </div>

          <div className="toolbar-group">
            <h3>Verification</h3>
            <div className="toolbar-buttons">
              <button
                type="button"
                className={verificationFilter === "" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => {
                  setVerificationFilter("");
                  setPage(1);
                }}
              >
                All Verification
              </button>
              <button
                type="button"
                className={verificationFilter === "pending" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => {
                  setVerificationFilter("pending");
                  setPage(1);
                }}
              >
                Pending
              </button>
              <button
                type="button"
                className={verificationFilter === "verified" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => {
                  setVerificationFilter("verified");
                  setPage(1);
                }}
              >
                Verified
              </button>
              <button
                type="button"
                className={verificationFilter === "rejected" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => {
                  setVerificationFilter("rejected");
                  setPage(1);
                }}
              >
                Rejected
              </button>
              <button
                type="button"
                className={
                  verificationFilter === "not_verified"
                    ? "btn btn-primary"
                    : "btn btn-light"
                }
                onClick={() => {
                  setVerificationFilter("not_verified");
                  setPage(1);
                }}
              >
                Not Verified
              </button>
            </div>
          </div>

          <div className="toolbar-group">
            <h3>Subscription</h3>
            <div className="toolbar-buttons">
              <button
                type="button"
                className={subscribedFilter === "" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => {
                  setSubscribedFilter("");
                  setPage(1);
                }}
              >
                Any Subscription
              </button>
              <button
                type="button"
                className={subscribedFilter === "true" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => {
                  setSubscribedFilter("true");
                  setPage(1);
                }}
              >
                Subscribed
              </button>
              <button
                type="button"
                className={subscribedFilter === "false" ? "btn btn-primary" : "btn btn-light"}
                onClick={() => {
                  setSubscribedFilter("false");
                  setPage(1);
                }}
              >
                Not Subscribed
              </button>
            </div>
          </div>

          <div className="toolbar-group">
            <h3>Search Providers</h3>
            <div className="search-row">
              <input
                type="text"
                className="search-input"
                placeholder="Search by name, business or email..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(1);
                }}
              />

              <button
                className="btn btn-light"
                onClick={() => {
                  setSearch("");
                  setStatusFilter("");
                  setVerificationFilter("");
                  setSubscribedFilter("");
                  setPage(1);
                }}
                type="button"
              >
                Clear
              </button>
            </div>
          </div>
        </section>

        <section className="card-ui provider-list-card">
          <div className="list-header">
            <h3>Showing {providers.length} providers</h3>
          </div>

          {loading ? (
            <div className="empty-box">Loading providers...</div>
          ) : providers.length === 0 ? (
            <div className="empty-box">No providers found</div>
          ) : (
            <div className="provider-list">
              {providers.map((provider) => {
                const verificationStatus =
                  provider.providerVerification?.status || "not_verified";

                return (
                  <div className="provider-row" key={provider.id}>
                    <div className="provider-left">
                      <div className="provider-avatar">
                        {(provider.businessName || provider.name || "?")
                          .charAt(0)
                          .toUpperCase()}
                      </div>

                      <div className="provider-details">
                        <strong>
                          {provider.businessName || provider.name || "Unnamed Provider"}
                        </strong>
                        <span>{provider.email || "No email"}</span>
                        <span>{provider.phone || "No phone"}</span>
                      </div>
                    </div>

                    <div className="provider-middle">
                      <span className={`pill account ${provider.status || "pending"}`}>
                        Account: {provider.status || "pending"}
                      </span>

                      <span className={`pill verification ${verificationStatus}`}>
                        Verification: {verificationStatus.replace(/_/g, " ")}
                      </span>

                      <span
                        className={`pill subscription ${
                          provider.subscription?.active ? "active" : "inactive"
                        }`}
                      >
                        {provider.subscription?.active
                          ? `Subscription: Active${
                              provider.subscription?.plan
                                ? ` (${provider.subscription.plan})`
                                : ""
                            }`
                          : "Subscription: Not Active"}
                      </span>
                    </div>

                    <div className="provider-right">
                      <button
                        className="btn btn-primary"
                        onClick={() => openProvider(provider.id)}
                        disabled={actionLoading}
                        type="button"
                      >
                        View
                      </button>

                      {provider.status === "pending" && (
                        <button
                          className="btn btn-success"
                          onClick={() => handleApproveAccount(provider.id)}
                          disabled={actionLoading}
                          type="button"
                        >
                          Approve
                        </button>
                      )}

                      {verificationStatus === "pending" && (
                        <>
                          <button
                            className="btn btn-success"
                            onClick={() => handleApproveVerification(provider.id)}
                            disabled={actionLoading}
                            type="button"
                          >
                            Verify
                          </button>

                          <button
                            className="btn btn-danger"
                            onClick={() => {
                              setSelectedProvider(provider);
                              setShowModal(true);
                              setShowRejectBox(true);
                              setRejectReason("");
                            }}
                            disabled={actionLoading}
                            type="button"
                          >
                            Reject
                          </button>
                        </>
                      )}

                      {!provider.subscription?.active &&
                        provider.status === "approved" && (
                          <button
                            className="btn btn-subscribe"
                            onClick={() => handleActivateSubscription(provider.id)}
                            disabled={actionLoading}
                            type="button"
                          >
                            Activate
                          </button>
                        )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        <div className="pagination">
          <button
            className="btn btn-light"
            disabled={page <= 1}
            onClick={() => setPage(page - 1)}
            type="button"
          >
            Prev
          </button>

          <span>
            Page {page} of {pages}
          </span>

          <button
            className="btn btn-light"
            disabled={page >= pages}
            onClick={() => setPage(page + 1)}
            type="button"
          >
            Next
          </button>
        </div>

        {showModal && selectedProvider && (
          <div className="modal-overlay" onClick={closeModal}>
            <div className="modal-card" onClick={(e) => e.stopPropagation()}>
              <div className="modal-head">
                <div>
                  <h3>
                    {selectedProvider.businessName ||
                      selectedProvider.name ||
                      "Provider"}
                  </h3>
                  <p>
                    {selectedProvider.businessType ||
                      selectedProvider.providerType ||
                      "Service Provider"}
                  </p>
                </div>

                {selectedProvider.providerVerification?.status === "verified" && (
                  <span className="verified-inline">VERIFIED PROVIDER</span>
                )}
              </div>

              <div className="modal-body">
                <div className="detail-box">
                  <span>Email</span>
                  <strong>{selectedProvider.email || "—"}</strong>
                </div>

                <div className="detail-box">
                  <span>Phone</span>
                  <strong>{selectedProvider.phone || "—"}</strong>
                </div>

                <div className="detail-box">
                  <span>Business Name</span>
                  <strong>
                    {selectedProvider.businessName || selectedProvider.name || "—"}
                  </strong>
                </div>

                <div className="detail-box">
                  <span>Address</span>
                  <strong>{selectedProvider.address || "—"}</strong>
                </div>

                <div className="detail-box">
                  <span>Account Status</span>
                  <strong>{renderAccountBadge(selectedProvider.status)}</strong>
                </div>

                <div className="detail-box">
                  <span>Verification Status</span>
                  <strong>
                    {renderVerificationBadge(
                      selectedProvider.providerVerification?.status
                    )}
                  </strong>
                </div>

                <div className="detail-box">
                  <span>Submitted At</span>
                  <strong>
                    {selectedProvider.providerVerification?.submittedAt
                      ? new Date(
                          selectedProvider.providerVerification.submittedAt
                        ).toLocaleString()
                      : "—"}
                  </strong>
                </div>

                <div className="detail-box">
                  <span>Reviewed At</span>
                  <strong>
                    {selectedProvider.providerVerification?.reviewedAt
                      ? new Date(
                          selectedProvider.providerVerification.reviewedAt
                        ).toLocaleString()
                      : "—"}
                  </strong>
                </div>

                <div className="detail-box">
                  <span>Subscription Plan</span>
                  <strong>
                    {selectedProvider.subscription?.active
                      ? selectedProvider.subscription?.plan || "Active"
                      : "Not Subscribed"}
                  </strong>
                </div>

                <div className="detail-box">
                  <span>Expiry Date</span>
                  <strong>
                    {selectedProvider.subscription?.expiryDate
                      ? new Date(
                          selectedProvider.subscription.expiryDate
                        ).toLocaleDateString()
                      : "—"}
                  </strong>
                </div>

                {selectedProvider.providerVerification?.rejectionReason && (
                  <div className="detail-box full">
                    <span>Rejection Reason</span>
                    <strong>
                      {selectedProvider.providerVerification.rejectionReason}
                    </strong>
                  </div>
                )}

                <div className="document-box full">
                  <div className="document-head">
                    <span>Verification Documents</span>
                  </div>

                  <div className="document-list">
                    <div className="document-item">
                      <span>Work License</span>
                      {selectedProvider.providerVerification?.licenseDocumentUrl ? (
                        <a
                          href={selectedProvider.providerVerification.licenseDocumentUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          View Document
                        </a>
                      ) : (
                        <em>Not uploaded</em>
                      )}
                    </div>

                    <div className="document-item">
                      <span>Business Registration</span>
                      {selectedProvider.providerVerification?.businessDocumentUrl ? (
                        <a
                          href={selectedProvider.providerVerification.businessDocumentUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          View Document
                        </a>
                      ) : (
                        <em>Not uploaded</em>
                      )}
                    </div>

                    <div className="document-item">
                      <span>Profile Image</span>
                      {selectedProvider.providerVerification?.profileImageUrl ? (
                        <a
                          href={selectedProvider.providerVerification.profileImageUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          View Image
                        </a>
                      ) : (
                        <em>Not uploaded</em>
                      )}
                    </div>
                  </div>

                  {selectedProvider.providerVerification?.profileImageUrl && (
                    <div
                      className="document-preview"
                      style={{ marginTop: "14px" }}
                    >
                      <img
                        src={selectedProvider.providerVerification.profileImageUrl}
                        alt="Provider profile"
                        style={{
                          width: "120px",
                          height: "120px",
                          objectFit: "cover",
                          borderRadius: "14px",
                          border: "1px solid #e5e7eb",
                        }}
                      />
                    </div>
                  )}
                </div>

                {showRejectBox && (
                  <div className="reject-box full">
                    <label htmlFor="rejectReason">Rejection Reason</label>
                    <textarea
                      id="rejectReason"
                      rows={4}
                      value={rejectReason}
                      onChange={(e) => setRejectReason(e.target.value)}
                      placeholder="Enter reason for rejection..."
                    />
                  </div>
                )}
              </div>

              <div className="modal-actions">
                {selectedProvider.status === "pending" && (
                  <button
                    className="btn btn-success"
                    onClick={() => handleApproveAccount(selectedProvider.id)}
                    disabled={actionLoading}
                    type="button"
                  >
                    Approve Account
                  </button>
                )}

                {selectedProvider.providerVerification?.status === "pending" && (
                  <>
                    <button
                      className="btn btn-success"
                      onClick={() => handleApproveVerification(selectedProvider.id)}
                      disabled={actionLoading}
                      type="button"
                    >
                      Approve Verification
                    </button>

                    {!showRejectBox ? (
                      <button
                        className="btn btn-danger"
                        onClick={() => setShowRejectBox(true)}
                        disabled={actionLoading}
                        type="button"
                      >
                        Reject Verification
                      </button>
                    ) : (
                      <button
                        className="btn btn-danger"
                        onClick={() => handleRejectVerification(selectedProvider.id)}
                        disabled={actionLoading}
                        type="button"
                      >
                        Confirm Reject
                      </button>
                    )}
                  </>
                )}

                {!selectedProvider.subscription?.active &&
                  selectedProvider.status === "approved" && (
                    <button
                      className="btn btn-subscribe"
                      onClick={() => handleActivateSubscription(selectedProvider.id)}
                      disabled={actionLoading}
                      type="button"
                    >
                      Activate Subscription
                    </button>
                  )}

                <button className="btn btn-light" onClick={closeModal} type="button">
                  Close
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}