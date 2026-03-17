import React from "react";
import { Link } from "react-router-dom";
import "./ProviderRejected.css";

export default function ProviderRejected() {
  return (
    <div className="rejected-container">
      <div className="rejected-card">
        <h1>Access Denied</h1>
        <p>
          Your provider account has been <span className="status-rejected">rejected</span>.
        </p>
        <p>
          Please contact support if you believe this is a mistake or for more information.
        </p>
        <Link to="/" className="btn-home">
          Back to Home
        </Link>
      </div>
    </div>
  );
}
