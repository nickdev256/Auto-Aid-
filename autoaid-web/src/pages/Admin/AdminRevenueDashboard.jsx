import { useEffect, useMemo, useState } from "react";
import axios from "axios";
import "./AdminRevenueDashboard.css";

const API_BASE =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:5001";

const api = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const token =
    localStorage.getItem("token") ||
    localStorage.getItem("adminToken") ||
    localStorage.getItem("autoaid_token");

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

function formatMoney(amount) {
  const value = Number(amount || 0);
  return `UGX ${value.toLocaleString()}`;
}

function formatDate(value) {
  if (!value) return "-";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString();
}

function getPaymentStatusClass(status) {
  const s = String(status || "").toLowerCase();
  if (s === "released") return "admin-revenue-status-released";
  if (s === "held_in_escrow") return "admin-revenue-status-escrow";
  if (s === "refunded") return "admin-revenue-status-refunded";
  return "admin-revenue-status-unpaid";
}

export default function AdminRevenueDashboard() {
  const [items, setItems] = useState([]);
  const [summary, setSummary] = useState({
    totalSystemFees: 0,
    totalSubscriptionRevenue: 0,
    totalRevenue: 0,
    escrowTotal: 0,
    completedCount: 0,
    awaitingConfirmationCount: 0,
  });

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");

  const loadRevenue = async (isRefresh = false) => {
    try {
      if (isRefresh) setRefreshing(true);
      else setLoading(true);

      setError("");

      const res = await api.get("/api/admin/revenue-dashboard");

      setSummary({
        totalSystemFees: Number(res.data?.summary?.totalSystemFees || 0),
        totalSubscriptionRevenue: Number(
          res.data?.summary?.totalSubscriptionRevenue || 0
        ),
        totalRevenue: Number(res.data?.summary?.totalRevenue || 0),
        escrowTotal: Number(res.data?.summary?.escrowTotal || 0),
        completedCount: Number(res.data?.summary?.completedCount || 0),
        awaitingConfirmationCount: Number(
          res.data?.summary?.awaitingConfirmationCount || 0
        ),
      });

      setItems(Array.isArray(res.data?.requests) ? res.data.requests : []);
    } catch (err) {
      if (err?.response?.status === 401) {
        setError("Unauthorized. Please login with an admin account.");
      } else if (err?.response?.status === 403) {
        setError("Access denied. This page requires admin privileges.");
      } else {
        setError(
          err?.response?.data?.message ||
            err?.message ||
            "Failed to load revenue dashboard"
        );
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadRevenue();
  }, []);

  const filteredItems = useMemo(() => {
    const q = search.trim().toLowerCase();

    return items.filter((item) => {
      const paymentStatus = String(item?.paymentStatus || "").toLowerCase();
      const providerName = String(
        item?.assignedProviderName || ""
      ).toLowerCase();
      const userName = String(item?.userName || "").toLowerCase();
      const service = String(item?.service || "").toLowerCase();
      const requestId = String(item?._id || "").toLowerCase();

      const matchesSearch =
        !q ||
        providerName.includes(q) ||
        userName.includes(q) ||
        service.includes(q) ||
        requestId.includes(q);

      const matchesStatus =
        statusFilter === "all" || paymentStatus === statusFilter;

      return matchesSearch && matchesStatus;
    });
  }, [items, search, statusFilter]);

  return (
    <div className="admin-revenue-page">
      <div className="admin-revenue-container">
        <div className="admin-revenue-header">
          <div>
            <h1 className="admin-revenue-title">Admin Revenue Dashboard</h1>
            <p className="admin-revenue-subtitle">
              Track system fees, subscription revenue, escrow money, and
              completed jobs.
            </p>
          </div>

          <button
            onClick={() => loadRevenue(true)}
            disabled={refreshing || loading}
            className="admin-revenue-refresh-btn"
          >
            {refreshing ? "Refreshing..." : "Refresh"}
          </button>
        </div>

        {error ? (
          <div className="admin-revenue-card admin-revenue-alert admin-revenue-alert-error">
            {error}
          </div>
        ) : null}

        <div className="admin-revenue-summary-grid">
          <div className="admin-revenue-card">
            <div className="admin-revenue-summary-label">System Fees</div>
            <div className="admin-revenue-summary-value revenue">
              {formatMoney(summary.totalSystemFees)}
            </div>
          </div>

          <div className="admin-revenue-card">
            <div className="admin-revenue-summary-label">Subscriptions</div>
            <div className="admin-revenue-summary-value subscriptions">
              {formatMoney(summary.totalSubscriptionRevenue)}
            </div>
          </div>

          <div className="admin-revenue-card">
            <div className="admin-revenue-summary-label">Total Revenue</div>
            <div className="admin-revenue-summary-value">
              {formatMoney(summary.totalRevenue)}
            </div>
          </div>

          <div className="admin-revenue-card">
            <div className="admin-revenue-summary-label">Escrow Total</div>
            <div className="admin-revenue-summary-value escrow">
              {formatMoney(summary.escrowTotal)}
            </div>
          </div>

          <div className="admin-revenue-card">
            <div className="admin-revenue-summary-label">Completed Jobs</div>
            <div className="admin-revenue-summary-value">
              {summary.completedCount}
            </div>
          </div>

          <div className="admin-revenue-card">
            <div className="admin-revenue-summary-label">
              Awaiting Confirmation
            </div>
            <div className="admin-revenue-summary-value pending">
              {summary.awaitingConfirmationCount}
            </div>
          </div>
        </div>

        <div className="admin-revenue-card admin-revenue-filters">
          <input
            type="text"
            placeholder="Search by provider, user, service, or request ID"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="admin-revenue-search"
          />

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="admin-revenue-select"
          >
            <option value="all">All payment statuses</option>
            <option value="unpaid">Unpaid</option>
            <option value="held_in_escrow">Held in Escrow</option>
            <option value="released">Released</option>
            <option value="refunded">Refunded</option>
          </select>
        </div>

        {loading ? (
          <div className="admin-revenue-card admin-revenue-state-box">
            Loading revenue dashboard...
          </div>
        ) : filteredItems.length === 0 ? (
          <div className="admin-revenue-card admin-revenue-state-box">
            No revenue records found.
          </div>
        ) : (
          <div className="admin-revenue-list">
            {filteredItems.map((item) => {
              const id = item?._id || item?.id;
              const paymentStatus = String(
                item?.paymentStatus || "unpaid"
              ).toLowerCase();

              return (
                <div key={id} className="admin-revenue-card">
                  <div className="admin-revenue-item-top">
                    <div>
                      <div className="admin-revenue-amount">
                        {formatMoney(item?.totalAmount)}
                      </div>

                      <div
                        className={`admin-revenue-status-chip ${getPaymentStatusClass(
                          paymentStatus
                        )}`}
                      >
                        {paymentStatus.replaceAll("_", " ")}
                      </div>
                    </div>

                    <div className="admin-revenue-meta">
                      <div>Created: {formatDate(item?.createdAt)}</div>
                      <div>Paid: {formatDate(item?.paidAt)}</div>
                      <div>ID: {id}</div>
                    </div>
                  </div>

                  <div className="admin-revenue-details-grid">
                    <div>
                      <div className="admin-revenue-section-title">Request</div>
                      <div className="admin-revenue-detail-line">
                        User: {item?.userName || "-"}
                      </div>
                      <div className="admin-revenue-detail-line">
                        Provider: {item?.assignedProviderName || "-"}
                      </div>
                      <div className="admin-revenue-detail-line">
                        Service: {item?.service || "-"}
                      </div>
                      <div className="admin-revenue-detail-line">
                        Status: {item?.status || "-"}
                      </div>
                    </div>

                    <div>
                      <div className="admin-revenue-section-title">
                        Revenue Split
                      </div>
                      <div className="admin-revenue-detail-line">
                        Total Paid: {formatMoney(item?.totalAmount)}
                      </div>
                      <div className="admin-revenue-detail-line">
                        System Fee: {formatMoney(item?.systemFee)}
                      </div>
                      <div className="admin-revenue-detail-line">
                        Provider Share: {formatMoney(item?.providerAmount)}
                      </div>
                      <div className="admin-revenue-detail-line">
                        Provider Completed:{" "}
                        {item?.providerCompleted ? "Yes" : "No"}
                      </div>
                      <div className="admin-revenue-detail-line">
                        User Completed: {item?.userCompleted ? "Yes" : "No"}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}