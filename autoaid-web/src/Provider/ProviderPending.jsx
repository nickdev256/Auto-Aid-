// src/Provider/ProviderPending.jsx
import React from "react";
import { Link } from "react-router-dom";
import "./ProviderPending.css";

export default function ProviderPending() {
  return (
    <div className="pending-container">
      <div className="pending-card">
        <h1>Account Pending</h1>
        <p>
          Your provider account is currently <span className="status-pending">under review</span>.
        </p>
        <p>
          Please wait for admin approval. You will be notified once your account is approved.
        </p>
        <Link to="/" className="btn-home">
          Back to Home
        </Link>
      </div>
    </div>
  );
}
