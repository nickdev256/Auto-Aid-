import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
  FiArrowLeft,
  FiCheckCircle,
  FiClock,
  FiCreditCard,
  FiEye,
  FiRefreshCw,
  FiSearch,
  FiShield,
  FiUserCheck,
  FiUsers,
  FiXCircle,
  FiFileText,
} from "react-icons/fi";
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
      <span className={`pm-pill verification ${safe}`}>
        {safe.replace(/_/g, " ").toUpperCase()}
      </span>
    );
  };

  const renderAccountBadge = (status) => {
    const safe = status || "pending";
    return <span className={`pm-pill account ${safe}`}>{safe.toUpperCase()}</span>;
  };

  return (
    <div className="pm-page">
      <main className="pm-container">
        <section className="pm-hero">
          <div className="pm-hero-left">
            <span className="pm-kicker">Admin / Provider Control</span>
            <h1>Provider Management</h1>
            <p>
              Review provider accounts, approve verification documents, manage
              subscriptions, and keep the AutoAid provider network organized.
            </p>

            <div className="pm-hero-mini">
              <div className="pm-mini-box">
                <span>Pending Verification</span>
                <strong>{stats.pendingVerification}</strong>
              </div>
              <div className="pm-mini-box">
                <span>Approved Accounts</span>
                <strong>{stats.approvedAccounts}</strong>
              </div>
              <div className="pm-mini-box">
                <span>Active Subscriptions</span>
                <strong>{stats.activeSubscriptions}</strong>
              </div>
            </div>
          </div>

          <div className="pm-hero-right">
            <button className="pm-btn pm-btn-light" onClick={loadProviders} type="button">
              <FiRefreshCw />
              <span>Refresh</span>
            </button>

            <button
              className="pm-btn pm-btn-primary"
              onClick={() => navigate("/admin")}
              type="button"
            >
              <FiArrowLeft />
              <span>Back to Dashboard</span>
            </button>
          </div>
        </section>

        <section className="pm-stats-grid">
          <div className="pm-stat-card total">
            <div className="pm-stat-icon">
              <FiUsers />
            </div>
            <div>
              <span>Total Providers</span>
              <strong>{stats.total}</strong>
            </div>
          </div>

          <div className="pm-stat-card pending">
            <div className="pm-stat-icon">
              <FiClock />
            </div>
            <div>
              <span>Pending Accounts</span>
              <strong>{stats.pendingAccounts}</strong>
            </div>
          </div>

          <div className="pm-stat-card approved">
            <div className="pm-stat-icon">
              <FiUserCheck />
            </div>
            <div>
              <span>Approved Accounts</span>
              <strong>{stats.approvedAccounts}</strong>
            </div>
          </div>

          <div className="pm-stat-card subscribed">
            <div className="pm-stat-icon">
              <FiCreditCard />
            </div>
            <div>
              <span>Active Subscriptions</span>
              <strong>{stats.activeSubscriptions}</strong>
            </div>
          </div>

          <div className="pm-stat-card verify">
            <div className="pm-stat-icon">
              <FiShield />
            </div>
            <div>
              <span>Verification Pending</span>
              <strong>{stats.pendingVerification}</strong>
            </div>
          </div>

          <div className="pm-stat-card verified">
            <div className="pm-stat-icon">
              <FiCheckCircle />
            </div>
            <div>
              <span>Verified Providers</span>
              <strong>{stats.verifiedProviders}</strong>
            </div>
          </div>
        </section>

        <section className="pm-filters-card">
          <div className="pm-filters-top">
            <div>
              <span className="pm-section-label">Filter Providers</span>
              <h3>Search and Segment</h3>
            </div>

            <div className="pm-search-wrap">
              <FiSearch />
              <input
                type="text"
                className="pm-search-input"
                placeholder="Search by name, business, phone or email..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(1);
                }}
              />
            </div>
          </div>

          <div className="pm-filter-groups">
            <div className="pm-filter-group">
              <h4>Account Status</h4>
              <div className="pm-chip-row">
                <button
                  type="button"
                  className={statusFilter === "" ? "pm-chip active" : "pm-chip"}
                  onClick={() => {
                    setStatusFilter("");
                    setPage(1);
                  }}
                >
                  All
                </button>
                <button
                  type="button"
                  className={statusFilter === "pending" ? "pm-chip active" : "pm-chip"}
                  onClick={() => {
                    setStatusFilter("pending");
                    setPage(1);
                  }}
                >
                  Pending
                </button>
                <button
                  type="button"
                  className={statusFilter === "approved" ? "pm-chip active" : "pm-chip"}
                  onClick={() => {
                    setStatusFilter("approved");
                    setPage(1);
                  }}
                >
                  Approved
                </button>
                <button
                  type="button"
                  className={statusFilter === "inactive" ? "pm-chip active" : "pm-chip"}
                  onClick={() => {
                    setStatusFilter("inactive");
                    setPage(1);
                  }}
                >
                  Inactive
                </button>
              </div>
            </div>

            <div className="pm-filter-group">
              <h4>Verification</h4>
              <div className="pm-chip-row">
                <button
                  type="button"
                  className={verificationFilter === "" ? "pm-chip active" : "pm-chip"}
                  onClick={() => {
                    setVerificationFilter("");
                    setPage(1);
                  }}
                >
                  All
                </button>
                <button
                  type="button"
                  className={verificationFilter === "pending" ? "pm-chip active" : "pm-chip"}
                  onClick={() => {
                    setVerificationFilter("pending");
                    setPage(1);
                  }}
                >
                  Pending
                </button>
                <button
                  type="button"
                  className={verificationFilter === "verified" ? "pm-chip active" : "pm-chip"}
                  onClick={() => {
                    setVerificationFilter("verified");
                    setPage(1);
                  }}
                >
                  Verified
                </button>
                <button
                  type="button"
                  className={verificationFilter === "rejected" ? "pm-chip active" : "pm-chip"}
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
                    verificationFilter === "not_verified" ? "pm-chip active" : "pm-chip"
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

            <div className="pm-filter-group">
              <h4>Subscription</h4>
              <div className="pm-chip-row">
                <button
                  type="button"
                  className={subscribedFilter === "" ? "pm-chip active" : "pm-chip"}
                  onClick={() => {
                    setSubscribedFilter("");
                    setPage(1);
                  }}
                >
                  Any
                </button>
                <button
                  type="button"
                  className={subscribedFilter === "true" ? "pm-chip active" : "pm-chip"}
                  onClick={() => {
                    setSubscribedFilter("true");
                    setPage(1);
                  }}
                >
                  Subscribed
                </button>
                <button
                  type="button"
                  className={subscribedFilter === "false" ? "pm-chip active" : "pm-chip"}
                  onClick={() => {
                    setSubscribedFilter("false");
                    setPage(1);
                  }}
                >
                  Not Subscribed
                </button>
              </div>
            </div>

            <div className="pm-filter-actions">
              <button
                className="pm-btn pm-btn-light"
                onClick={() => {
                  setSearch("");
                  setStatusFilter("");
                  setVerificationFilter("");
                  setSubscribedFilter("");
                  setPage(1);
                }}
                type="button"
              >
                Clear Filters
              </button>
            </div>
          </div>
        </section>

        <section className="pm-table-card">
          <div className="pm-table-head">
            <div>
              <span className="pm-section-label">Provider Records</span>
              <h3>Showing {providers.length} providers</h3>
            </div>
          </div>

          {loading ? (
            <div className="pm-empty-box">Loading providers...</div>
          ) : providers.length === 0 ? (
            <div className="pm-empty-box">No providers found</div>
          ) : (
            <div className="pm-provider-list">
              {providers.map((provider) => {
                const verificationStatus =
                  provider.providerVerification?.status || "not_verified";

                return (
                  <div className="pm-provider-row" key={provider.id}>
                    <div className="pm-provider-main">
                      <div className="pm-provider-avatar">
                        {(provider.businessName || provider.name || "?")
                          .charAt(0)
                          .toUpperCase()}
                      </div>

                      <div className="pm-provider-info">
                        <h4>
                          {provider.businessName || provider.name || "Unnamed Provider"}
                        </h4>
                        <p>{provider.businessType || provider.providerType || "Provider"}</p>

                        <div className="pm-provider-meta">
                          <span>{provider.email || "No email"}</span>
                          <span className="pm-dot">•</span>
                          <span>{provider.phone || "No phone"}</span>
                        </div>
                      </div>
                    </div>

                    <div className="pm-provider-tags">
                      <span className={`pm-pill account ${provider.status || "pending"}`}>
                        Account: {provider.status || "pending"}
                      </span>

                      <span className={`pm-pill verification ${verificationStatus}`}>
                        Verification: {verificationStatus.replace(/_/g, " ")}
                      </span>

                      <span
                        className={`pm-pill subscription ${
                          provider.subscription?.active ? "active" : "inactive"
                        }`}
                      >
                        {provider.subscription?.active
                          ? `Subscription${
                              provider.subscription?.plan
                                ? `: ${provider.subscription.plan}`
                                : ": Active"
                            }`
                          : "Subscription: Not Active"}
                      </span>
                    </div>

                    <div className="pm-provider-actions">
                      <button
                        className="pm-btn pm-btn-primary"
                        onClick={() => openProvider(provider.id)}
                        disabled={actionLoading}
                        type="button"
                      >
                        <FiEye />
                        <span>View</span>
                      </button>

                      {provider.status === "pending" && (
                        <button
                          className="pm-btn pm-btn-success"
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
                            className="pm-btn pm-btn-success"
                            onClick={() => handleApproveVerification(provider.id)}
                            disabled={actionLoading}
                            type="button"
                          >
                            Verify
                          </button>

                          <button
                            className="pm-btn pm-btn-danger"
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
                            className="pm-btn pm-btn-subscribe"
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

        <div className="pm-pagination">
          <button
            className="pm-btn pm-btn-light"
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
            className="pm-btn pm-btn-light"
            disabled={page >= pages}
            onClick={() => setPage(page + 1)}
            type="button"
          >
            Next
          </button>
        </div>

        {showModal && selectedProvider && (
          <div className="pm-modal-overlay" onClick={closeModal}>
            <div className="pm-modal-card" onClick={(e) => e.stopPropagation()}>
              <div className="pm-modal-head">
                <div className="pm-modal-title">
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
                  <span className="pm-verified-inline">VERIFIED PROVIDER</span>
                )}
              </div>

              <div className="pm-modal-body">
                <div className="pm-detail-box">
                  <span>Email</span>
                  <strong>{selectedProvider.email || "—"}</strong>
                </div>

                <div className="pm-detail-box">
                  <span>Phone</span>
                  <strong>{selectedProvider.phone || "—"}</strong>
                </div>

                <div className="pm-detail-box">
                  <span>Business Name</span>
                  <strong>
                    {selectedProvider.businessName || selectedProvider.name || "—"}
                  </strong>
                </div>

                <div className="pm-detail-box">
                  <span>Address</span>
                  <strong>{selectedProvider.address || "—"}</strong>
                </div>

                <div className="pm-detail-box">
                  <span>Account Status</span>
                  <strong>{renderAccountBadge(selectedProvider.status)}</strong>
                </div>

                <div className="pm-detail-box">
                  <span>Verification Status</span>
                  <strong>
                    {renderVerificationBadge(
                      selectedProvider.providerVerification?.status
                    )}
                  </strong>
                </div>

                <div className="pm-detail-box">
                  <span>Submitted At</span>
                  <strong>
                    {selectedProvider.providerVerification?.submittedAt
                      ? new Date(
                          selectedProvider.providerVerification.submittedAt
                        ).toLocaleString()
                      : "—"}
                  </strong>
                </div>

                <div className="pm-detail-box">
                  <span>Reviewed At</span>
                  <strong>
                    {selectedProvider.providerVerification?.reviewedAt
                      ? new Date(
                          selectedProvider.providerVerification.reviewedAt
                        ).toLocaleString()
                      : "—"}
                  </strong>
                </div>

                <div className="pm-detail-box">
                  <span>Subscription Plan</span>
                  <strong>
                    {selectedProvider.subscription?.active
                      ? selectedProvider.subscription?.plan || "Active"
                      : "Not Subscribed"}
                  </strong>
                </div>

                <div className="pm-detail-box">
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
                  <div className="pm-detail-box full">
                    <span>Rejection Reason</span>
                    <strong>
                      {selectedProvider.providerVerification.rejectionReason}
                    </strong>
                  </div>
                )}

                <div className="pm-document-box full">
                  <div className="pm-document-head">
                    <span>Verification Documents</span>
                  </div>

                  <div className="pm-document-list">
                    <div className="pm-document-item">
                      <div className="pm-doc-label">
                        <FiFileText />
                        <span>Work License</span>
                      </div>
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

                    <div className="pm-document-item">
                      <div className="pm-doc-label">
                        <FiFileText />
                        <span>Business Registration</span>
                      </div>
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

                    <div className="pm-document-item">
                      <div className="pm-doc-label">
                        <FiFileText />
                        <span>Profile Image</span>
                      </div>
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
                    <div className="pm-document-preview">
                      <img
                        src={selectedProvider.providerVerification.profileImageUrl}
                        alt="Provider profile"
                      />
                    </div>
                  )}
                </div>

                {showRejectBox && (
                  <div className="pm-reject-box full">
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

              <div className="pm-modal-actions">
                {selectedProvider.status === "pending" && (
                  <button
                    className="pm-btn pm-btn-success"
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
                      className="pm-btn pm-btn-success"
                      onClick={() => handleApproveVerification(selectedProvider.id)}
                      disabled={actionLoading}
                      type="button"
                    >
                      Approve Verification
                    </button>

                    {!showRejectBox ? (
                      <button
                        className="pm-btn pm-btn-danger"
                        onClick={() => setShowRejectBox(true)}
                        disabled={actionLoading}
                        type="button"
                      >
                        Reject Verification
                      </button>
                    ) : (
                      <button
                        className="pm-btn pm-btn-danger"
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
                      className="pm-btn pm-btn-subscribe"
                      onClick={() => handleActivateSubscription(selectedProvider.id)}
                      disabled={actionLoading}
                      type="button"
                    >
                      Activate Subscription
                    </button>
                  )}

                <button className="pm-btn pm-btn-light" onClick={closeModal} type="button">
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