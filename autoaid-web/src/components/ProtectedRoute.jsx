import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute({ children, role }) {
  const { user } = useAuth();

  if (!user) return <Navigate to="/login" replace />;

  if (role && user.role !== role) {
    if (user.role === "user") return <Navigate to="/dashboard" replace />;
    if (user.role === "admin") return <Navigate to="/admin" replace />;

    if (user.role === "provider") {
      if (user.status === "pending")
        return <Navigate to="/provider/pending" replace />;
      if (user.status === "rejected")
        return <Navigate to="/provider/rejected" replace />;
      return <Navigate to="/provider/dashboard" replace />;
    }
  }

  return children;
}
