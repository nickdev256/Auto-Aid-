// ✅ FILE NAME: src/api.js
// ✅ FULL UPDATED VERSION
// ✅ Adds maintenance mode handling for WEB
//    - if backend returns 503, redirect to /maintenance
// ✅ Still cookie-based auth: credentials: "include"

const BASE = (import.meta.env.VITE_API_URL || "http://localhost:5001").replace(/\/$/, "");

// -----------------------------
// Core request helper
// -----------------------------
async function request(path, options = {}) {
  const url = path.startsWith("http") ? path : `${BASE}${path}`;

  const res = await fetch(url, {
    credentials: "include",
    ...options,
    headers: {
      ...(options.body && !(options.body instanceof FormData)
        ? { "Content-Type": "application/json" }
        : {}),
      "X-Client": "web",
      ...(options.headers || {}),
    },
  });

  let data = null;
  const contentType = res.headers.get("content-type") || "";

  try {
    if (contentType.includes("application/json")) data = await res.json();
    else data = await res.text();
  } catch {
    data = null;
  }

  // ✅ GLOBAL MAINTENANCE MODE HANDLER
  if (res.status === 503) {
    const message =
      (data && typeof data === "object" && data.message) ||
      "AutoAid is currently under maintenance. Please try again later.";

    try {
      sessionStorage.setItem(
        "maintenance_payload",
        JSON.stringify(
          typeof data === "object" && data
            ? data
            : {
                maintenanceMode: true,
                message,
                systemName: "AutoAid",
              }
        )
      );
    } catch {
      // ignore storage failure
    }

    // Avoid redirect loop if already on maintenance page
    if (typeof window !== "undefined" && window.location.pathname !== "/maintenance") {
      window.location.href = "/maintenance";
    }

    const err = new Error(message);
    err.status = 503;
    err.code = "MAINTENANCE_MODE";
    err.data = data;
    throw err;
  }

  if (!res.ok) {
    const message =
      (data && typeof data === "object" && data.message) ||
      (typeof data === "string" && data) ||
      res.statusText ||
      "Request failed";

    const err = new Error(message);
    err.status = res.status;
    err.data = data;

    if (res.status === 401) err.code = "SESSION_EXPIRED";
    throw err;
  }

  return data;
}

// -----------------------------
// Auth
// -----------------------------
export const signupAuth = (payload) =>
  request("/api/auth/signup", {
    method: "POST",
    body: JSON.stringify(payload),
  });

export const loginAuth = (payload) =>
  request("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });

export const meAuth = () => request("/api/auth/me", { method: "GET" });

export const logoutAuth = () => request("/api/auth/logout", { method: "POST" });

// -----------------------------
// Admin
// -----------------------------
export const getAllUsers = (from = "all") => {
  const qs = from && from !== "all" ? `?from=${encodeURIComponent(from)}` : "";
  return request(`/api/admin/users${qs}`);
};

export const getAdminUsers = getAllUsers;

export const getPendingProviders = () => request("/api/admin/providers/pending");

export const getPendingProvidersAlt = () => request("/api/admin/pending-providers");

export const approveProvider = (id) =>
  request(`/api/admin/approve/${id}`, { method: "PUT" });

export const activateSubscription = (id, plan) =>
  request(`/api/admin/subscribe/${id}`, {
    method: "PUT",
    body: JSON.stringify(plan ? { plan } : {}),
  });

export const getProviders = ({
  page = 1,
  limit = 10,
  search = "",
  status = "",
  subscribed = "",
} = {}) => {
  const qs = new URLSearchParams({
    page: String(page),
    limit: String(limit),
    search: search ?? "",
    status: status ?? "",
    subscribed: subscribed ?? "",
  });
  return request(`/api/admin/providers?${qs.toString()}`);
};

export const getProviderById = (id) => request(`/api/admin/providers/${id}`);

export const getAdminStats = () => request("/api/admin/stats");

export const getAdminSettings = () => request("/api/admin/settings");

export const updateAdminSettings = (payload) =>
  request("/api/admin/settings", {
    method: "PUT",
    body: JSON.stringify(payload),
  });

export const getAllServiceRequests = (from = "all") => {
  const qs = from && from !== "all" ? `?from=${encodeURIComponent(from)}` : "";
  return request(`/api/admin/service-requests${qs}`);
};

export const getServiceRequests = (from = "all") => getAllServiceRequests(from);

export const updateServiceRequest = (id, status) =>
  request(`/api/admin/service-requests/${id}`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });

// -----------------------------
// Garage Routes
// -----------------------------
export const getNearbyGarages = (coords) =>
  request("/api/garage/nearby", {
    method: "POST",
    body: JSON.stringify(coords),
  });

export const createGarageRequest = (payload) =>
  request("/api/garage/request", {
    method: "POST",
    body: JSON.stringify(payload),
  });

export const getRequestsByProvider = (providerId) =>
  request(`/api/garage/byProvider/${providerId}`);

export const updateRequestStatus = (requestId, status) =>
  request(`/api/garage/${requestId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });

// -----------------------------
// Provider Profile Settings
// -----------------------------
export const updateProviderBusiness = (id, data) =>
  request(`/api/providers/provider/${id}/settings`, {
    method: "PUT",
    body: JSON.stringify(data),
  });

export const getProviderSubscriptionStatus = (providerId) =>
  request(`/api/providers/${providerId}/subscription`);

export const getPublicProviderProfile = (id) =>
  request(`/api/providers/public/${id}`, { method: "GET" });

export const updateProviderProfile = (payload) =>
  request("/api/provider/update", {
    method: "PUT",
    body: JSON.stringify(payload),
  });

// -----------------------------
// Subscriptions
// -----------------------------
export const getSubscriptionPlans = () => request("/api/subscriptions/plans");

export const getProviderSubscription = (providerId) =>
  request(`/api/subscriptions/${providerId}`);

export const subscribeProvider = (providerId, planId, paymentMethod) =>
  request("/api/subscriptions/subscribe", {
    method: "POST",
    body: JSON.stringify({ providerId, planId, paymentMethod }),
  });

// -----------------------------
// Payments
// -----------------------------
export const startSubscriptionPayment = ({ providerId, planId, phone, network }) =>
  request("/api/payments/subscribe", {
    method: "POST",
    body: JSON.stringify({ providerId, planId, phone, network }),
  });

export const getPesapalPaymentStatus = (orderTrackingId) =>
  request(`/api/payments/status?orderTrackingId=${encodeURIComponent(orderTrackingId)}`);