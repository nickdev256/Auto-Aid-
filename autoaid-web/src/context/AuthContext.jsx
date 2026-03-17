import React, { createContext, useContext, useEffect, useState } from "react";
import axios from "axios";

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

const API = (import.meta.env.VITE_API_URL || "http://localhost:5001") + "/api/auth";

// allow cookies globally
axios.defaults.withCredentials = true;

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);

  const saveAuthData = (data) => {
    const token =
      data?.token ||
      data?.accessToken ||
      data?.jwt ||
      data?.user?.token ||
      null;

    const resolvedUser = data?.user || null;

    if (token) {
      localStorage.setItem("token", token);
    }

    if (resolvedUser) {
      localStorage.setItem("auth_user", JSON.stringify(resolvedUser));
    }
  };

  const clearAuthData = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("adminToken");
    localStorage.removeItem("autoaid_token");
    localStorage.removeItem("auth_user");
  };

  // ✅ Optional: check session on reload
  const checkAuth = async () => {
    try {
      const res = await axios.get(`${API}/me`, { withCredentials: true });
      const me = res.data.user || null;
      setUser(me);

      if (me) {
        localStorage.setItem("auth_user", JSON.stringify(me));
      }
    } catch {
      setUser(null);
      clearAuthData();
    } finally {
      setAuthLoading(false);
    }
  };

  useEffect(() => {
    checkAuth();
  }, []);

  const login = async (email, password) => {
    try {
      const res = await axios.post(
        `${API}/login`,
        {
          email: (email || "").trim().toLowerCase(),
          password: password || "",
        },
        { withCredentials: true }
      );

      saveAuthData(res.data);
      setUser(res.data.user || null);

      return res.data.user;
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        "Login failed";
      throw new Error(msg);
    }
  };

  const signup = async (formData) => {
    try {
      const res = await axios.post(`${API}/signup`, formData, {
        withCredentials: true,
      });
      return res.data;
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        "Signup failed";
      throw new Error(msg);
    }
  };

  const logout = async () => {
    try {
      await axios.post(`${API}/logout`, {}, { withCredentials: true });
    } catch {}
    clearAuthData();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, setUser, login, signup, logout, authLoading }}>
      {children}
    </AuthContext.Provider>
  );
};