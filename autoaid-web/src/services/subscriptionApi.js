// src/services/subscriptionApi.js
import axios from "axios";

const API = axios.create({
  baseURL: "http://localhost:5001/api",
  timeout: 10000,
});

export async function getPlans() {
  const r = await API.get("/subscriptions/plans");
  return r.data.plans || [];
}

export async function subscribeProvider({ providerId, planId, paymentMethod }) {
  const r = await API.post("/subscriptions/subscribe", { providerId, planId, paymentMethod });
  return r.data;
}

export async function getProviderSubscription(providerId) {
  const r = await API.get(`/provider/${providerId}/subscription`);
  return r.data;
}

/* Admin helpers */
export async function adminGetUsers() {
  const r = await API.get("/admin/users");
  return r.data || [];
}

export async function adminApproveProvider(id) {
  const r = await API.put(`/admin/approve/${id}`);
  return r.data;
}

export async function adminSubscribeProvider(id, plan) {
  const r = await API.put(`/admin/subscribe/${id}`, { plan });
  return r.data;
}
