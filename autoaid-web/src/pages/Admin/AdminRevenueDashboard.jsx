import { useEffect, useMemo, useState } from "react";
import axios from "axios";
import {
  FiActivity,
  FiArrowLeft,
  FiCheckCircle,
  FiClock,
  FiCreditCard,
  FiDollarSign,
  FiRefreshCw,
  FiSearch,
  FiShield,
} from "react-icons/fi";
import { useNavigate } from "react-router-dom";
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
  if (s === "released") return "ard-status-released";
  if (s === "held_in_escrow") return "ard-status-escrow";
  if (s === "refunded") return "ard-status-refunded";
  return "ard-status-unpaid";
}

function prettyPaymentStatus(status) {
  return String(status || "unpaid").replaceAll("_", " ");
}

export default function AdminRevenueDashboard() {
  const navigate = useNavigate();

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
    <div className="ard-page">
      <main className="ard-container">
        <section className="ard-hero">
          <div className="ard-hero-left">
            <span className="ard-kicker">Admin / Revenue Center</span>
            <h1>Revenue Dashboard</h1>
            <p>
              Track system fees, subscription income, escrow balances, completed
              jobs, and revenue split performance across AutoAid.
            </p>

            <div className="ard-hero-mini">
              <div className="ard-mini-box">
                <span>Total Revenue</span>
                <strong>{formatMoney(summary.totalRevenue)}</strong>
              </div>
              <div className="ard-mini-box">
                <span>Escrow Total</span>
                <strong>{formatMoney(summary.escrowTotal)}</strong>
              </div>
              <div className="ard-mini-box">
                <span>Completed Jobs</span>
                <strong>{summary.completedCount}</strong>
              </div>
            </div>
          </div>

          <div className="ard-hero-right">
            <button
              onClick={() => navigate("/admin")}
              className="ard-btn ard-btn-light"
              type="button"
            >
              <FiArrowLeft />
              <span>Back to Dashboard</span>
            </button>

            <button
              onClick={() => loadRevenue(true)}
              disabled={refreshing || loading}
              className="ard-btn ard-btn-primary"
              type="button"
            >
              <FiRefreshCw />
              <span>{refreshing ? "Refreshing..." : "Refresh"}</span>
            </button>
          </div>
        </section>

        {error ? (
          <div className="ard-alert ard-alert-error">{error}</div>
        ) : null}

        <section className="ard-stats-grid">
          <div className="ard-stat-card fees">
            <div className="ard-stat-icon">
              <FiDollarSign />
            </div>
            <div>
              <span>System Fees</span>
              <strong>{formatMoney(summary.totalSystemFees)}</strong>
            </div>
          </div>

          <div className="ard-stat-card subscriptions">
            <div className="ard-stat-icon">
              <FiCreditCard />
            </div>
            <div>
              <span>Subscriptions</span>
              <strong>{formatMoney(summary.totalSubscriptionRevenue)}</strong>
            </div>
          </div>

          <div className="ard-stat-card total">
            <div className="ard-stat-icon">
              <FiShield />
            </div>
            <div>
              <span>Total Revenue</span>
              <strong>{formatMoney(summary.totalRevenue)}</strong>
            </div>
          </div>

          <div className="ard-stat-card escrow">
            <div className="ard-stat-icon">
              <FiClock />
            </div>
            <div>
              <span>Escrow Total</span>
              <strong>{formatMoney(summary.escrowTotal)}</strong>
            </div>
          </div>

          <div className="ard-stat-card complete">
            <div className="ard-stat-icon">
              <FiCheckCircle />
            </div>
            <div>
              <span>Completed Jobs</span>
              <strong>{summary.completedCount}</strong>
            </div>
          </div>

          <div className="ard-stat-card pending">
            <div className="ard-stat-icon">
              <FiActivity />
            </div>
            <div>
              <span>Awaiting Confirmation</span>
              <strong>{summary.awaitingConfirmationCount}</strong>
            </div>
          </div>
        </section>

        <section className="ard-filter-card">
          <div className="ard-filter-top">
            <div>
              <span className="ard-section-label">Revenue Filters</span>
              <h3>Search and Segment</h3>
            </div>
          </div>

          <div className="ard-filter-grid">
            <div className="ard-search-wrap">
              <FiSearch />
              <input
                type="text"
                placeholder="Search by provider, user, service, or request ID"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="ard-search-input"
              />
            </div>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="ard-select"
            >
              <option value="all">All payment statuses</option>
              <option value="unpaid">Unpaid</option>
              <option value="held_in_escrow">Held in Escrow</option>
              <option value="released">Released</option>
              <option value="refunded">Refunded</option>
            </select>
          </div>
        </section>

        {loading ? (
          <div className="ard-state-box">Loading revenue dashboard...</div>
        ) : filteredItems.length === 0 ? (
          <div className="ard-state-box">No revenue records found.</div>
        ) : (
          <section className="ard-list">
            {filteredItems.map((item) => {
              const id = item?._id || item?.id;
              const paymentStatus = String(
                item?.paymentStatus || "unpaid"
              ).toLowerCase();

              return (
                <div key={id} className="ard-revenue-card">
                  <div className="ard-revenue-top">
                    <div className="ard-revenue-top-left">
                      <div className="ard-amount">
                        {formatMoney(item?.totalAmount)}
                      </div>

                      <div
                        className={`ard-status-chip ${getPaymentStatusClass(
                          paymentStatus
                        )}`}
                      >
                        {prettyPaymentStatus(paymentStatus)}
                      </div>
                    </div>

                    <div className="ard-meta">
                      <div>Created: {formatDate(item?.createdAt)}</div>
                      <div>Paid: {formatDate(item?.paidAt)}</div>
                      <div>ID: {id}</div>
                    </div>
                  </div>

                  <div className="ard-details-grid">
                    <div className="ard-detail-panel">
                      <div className="ard-panel-title">Request</div>
                      <div className="ard-detail-line">
                        User: {item?.userName || "-"}
                      </div>
                      <div className="ard-detail-line">
                        Provider: {item?.assignedProviderName || "-"}
                      </div>
                      <div className="ard-detail-line">
                        Service: {item?.service || "-"}
                      </div>
                      <div className="ard-detail-line">
                        Status: {item?.status || "-"}
                      </div>
                    </div>

                    <div className="ard-detail-panel">
                      <div className="ard-panel-title">Revenue Split</div>
                      <div className="ard-detail-line">
                        Total Paid: {formatMoney(item?.totalAmount)}
                      </div>
                      <div className="ard-detail-line">
                        System Fee: {formatMoney(item?.systemFee)}
                      </div>
                      <div className="ard-detail-line">
                        Provider Share: {formatMoney(item?.providerAmount)}
                      </div>
                      <div className="ard-detail-line">
                        Provider Completed:{" "}
                        {item?.providerCompleted ? "Yes" : "No"}
                      </div>
                      <div className="ard-detail-line">
                        User Completed: {item?.userCompleted ? "Yes" : "No"}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </section>
        )}
      </main>
    </div>
  );
}