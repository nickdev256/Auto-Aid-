import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./ServiceCard.css";

export default function ServiceCard({ icon, title, description, details, to, onClick }) {
  const [flipped, setFlipped] = useState(false);
  const { user } = useAuth();
  const navigate = useNavigate();

  const handleClick = () => setFlipped(!flipped);

  const handleCTA = (e) => {
    e.stopPropagation();

    if (!user) return navigate("/login");

    if (title === "Fuel Delivery") {
      return navigate("/fuel");   // 🔥 FIXED ROUTE
    }

    if (to) return navigate(to);

    if (onClick) return onClick();
  };

  return (
    <div className={`service-card ${flipped ? "flipped" : ""}`} onClick={handleClick}>

      {/* FRONT */}
      <div className="card-front">
        <div className="service-icon">{icon}</div>
        <h3 className="service-title">{title}</h3>
        <p className="service-description">{description}</p>
      </div>

      {/* BACK */}
      <div className="card-back">
        <h3>{title} Details</h3>
        <p>{details}</p>

        <button className="btn primary-btn" onClick={handleCTA}>
          {user ? (title === "Fuel Delivery" ? "Request Fuel" : "Proceed") : "Sign In to Continue"}
        </button>
      </div>

    </div>
  );
}
