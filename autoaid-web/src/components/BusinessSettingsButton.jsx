import React from "react";
import { useNavigate } from "react-router-dom";
import "./BusinessSettingsButton.css";

export default function BusinessSettingsButton() {
  const nav = useNavigate();

  return (
    <button
      className="business-settings-btn"
      onClick={() => nav("/provider/settings")}
      title="Business Settings"
    >
      ⚙
    </button>
  );
}
