// src/pages/Admin/Settings.jsx
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Settings.css";

const DEFAULT_MAINTENANCE_MESSAGE =
  "AutoAid is currently under maintenance. Please try again later.";

export default function Settings() {
  const navigate = useNavigate();

  const [systemName, setSystemName] = useState("");
  const [supportEmail, setSupportEmail] = useState("");
  const [notificationsEnabled, setNotificationsEnabled] = useState(false);
  const [maintenanceMode, setMaintenanceMode] = useState(false);
  const [maintenanceMessage, setMaintenanceMessage] = useState(
    DEFAULT_MAINTENANCE_MESSAGE
  );

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // keep original values so admin can reset unsaved edits
  const [initialData, setInitialData] = useState({
    systemName: "",
    supportEmail: "",
    notificationsEnabled: false,
    maintenanceMode: false,
    maintenanceMessage: DEFAULT_MAINTENANCE_MESSAGE,
  });

  const loadSettings = async () => {
    try {
      setLoading(true);
      setError("");
      setSuccess("");

      const res = await fetch("http://localhost:5001/api/admin/settings", {
        method: "GET",
        credentials: "include",
        headers: {
          "X-Client": "web",
        },
      });

      if (res.status === 401) {
        throw new Error("Unauthorized. Please login as admin.");
      }

      if (res.status === 403) {
        throw new Error("Access denied. Admin only.");
      }

      if (!res.ok) {
        const msg = await res.text().catch(() => "Failed to load settings");
        throw new Error(msg);
      }

      const data = await res.json();

      const normalized = {
        systemName: data.systemName || "",
        supportEmail: data.supportEmail || "",
        notificationsEnabled: !!data.notificationsEnabled,
        maintenanceMode: !!data.maintenanceMode,
        maintenanceMessage:
          data.maintenanceMessage || DEFAULT_MAINTENANCE_MESSAGE,
      };

      setSystemName(normalized.systemName);
      setSupportEmail(normalized.supportEmail);
      setNotificationsEnabled(normalized.notificationsEnabled);
      setMaintenanceMode(normalized.maintenanceMode);
      setMaintenanceMessage(normalized.maintenanceMessage);
      setInitialData(normalized);
    } catch (err) {
      console.error("Failed to load settings:", err);
      setError(err.message || "Failed to load settings");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSettings();
  }, []);

  const saveSettings = async () => {
    try {
      setSaving(true);
      setError("");
      setSuccess("");

      const res = await fetch("http://localhost:5001/api/admin/settings", {
        method: "PUT",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-Client": "web",
        },
        body: JSON.stringify({
          systemName,
          supportEmail,
          notificationsEnabled,
          maintenanceMode,
          maintenanceMessage,
        }),
      });

      if (res.status === 401) {
        throw new Error("Unauthorized. Please login as admin.");
      }

      if (res.status === 403) {
        throw new Error("Access denied. Admin only.");
      }

      if (!res.ok) {
        const msg = await res.text().catch(() => "Failed to save settings");
        throw new Error(msg);
      }

      setSuccess("Settings updated successfully.");

      setInitialData({
        systemName,
        supportEmail,
        notificationsEnabled,
        maintenanceMode,
        maintenanceMessage,
      });
    } catch (err) {
      console.error("Failed to save settings:", err);
      setError(err.message || "Failed to save settings");
    } finally {
      setSaving(false);
    }
  };

  const toggleMaintenanceQuickly = async () => {
    const nextValue = !maintenanceMode;
    setMaintenanceMode(nextValue);

    try {
      setSaving(true);
      setError("");
      setSuccess("");

      const res = await fetch(
        "http://localhost:5001/api/admin/settings/maintenance",
        {
          method: "PATCH",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-Client": "web",
          },
          body: JSON.stringify({
            maintenanceMode: nextValue,
            maintenanceMessage,
          }),
        }
      );

      if (res.status === 401) {
        throw new Error("Unauthorized. Please login as admin.");
      }

      if (res.status === 403) {
        throw new Error("Access denied. Admin only.");
      }

      if (!res.ok) {
        const msg = await res.text().catch(() => "Failed to toggle maintenance");
        throw new Error(msg);
      }

      setSuccess(
        `Maintenance mode ${nextValue ? "enabled" : "disabled"} successfully.`
      );

      setInitialData((prev) => ({
        ...prev,
        maintenanceMode: nextValue,
        maintenanceMessage,
      }));
    } catch (err) {
      console.error("Failed to toggle maintenance:", err);
      setMaintenanceMode(!nextValue);
      setError(err.message || "Failed to toggle maintenance");
    } finally {
      setSaving(false);
    }
  };

  const resetChanges = () => {
    setSystemName(initialData.systemName);
    setSupportEmail(initialData.supportEmail);
    setNotificationsEnabled(initialData.notificationsEnabled);
    setMaintenanceMode(initialData.maintenanceMode);
    setMaintenanceMessage(initialData.maintenanceMessage);
    setError("");
    setSuccess("Unsaved changes reset.");
  };

  const hasChanges =
    systemName !== initialData.systemName ||
    supportEmail !== initialData.supportEmail ||
    notificationsEnabled !== initialData.notificationsEnabled ||
    maintenanceMode !== initialData.maintenanceMode ||
    maintenanceMessage !== initialData.maintenanceMessage;

  if (loading) return <p>Loading settings...</p>;

  return (
    <div className="settings-container">
      <button className="back-btn" onClick={() => navigate(-1)}>
        Back
      </button>

      <h1 className="settings-title">System Settings </h1>

      {error && (
        <div
          style={{
            background: "#ffe5e5",
            color: "#b42318",
            border: "1px solid #f5b5b5",
            padding: "12px 14px",
            borderRadius: 8,
            marginBottom: 16,
          }}
        >
          {error}
        </div>
      )}

      {success && (
        <div
          style={{
            background: "#e8fff0",
            color: "#157347",
            border: "1px solid #b7ebc6",
            padding: "12px 14px",
            borderRadius: 8,
            marginBottom: 16,
          }}
        >
          {success}
        </div>
      )}

      <div className="settings-grid">
        <div className="settings-card">
          <h3>System Information</h3>

          <label>System Name</label>
          <input
            value={systemName}
            onChange={(e) => setSystemName(e.target.value)}
            placeholder="Enter system name"
          />

          <label>Support Email</label>
          <input
            type="email"
            value={supportEmail}
            onChange={(e) => setSupportEmail(e.target.value)}
            placeholder="Enter support email"
          />

          <div style={{ display: "flex", gap: 10, flexWrap: "wrap", marginTop: 16 }}>
            <button
              className="settings-btn"
              onClick={saveSettings}
              disabled={saving}
            >
              {saving ? "Saving..." : "Save Changes"}
            </button>

            <button
              className="settings-btn"
              onClick={loadSettings}
              disabled={saving}
              style={{ background: "#fff", color: "#1a2b4c", border: "1px solid #d0d7e6" }}
            >
              Reload
            </button>

            <button
              className="settings-btn"
              onClick={resetChanges}
              disabled={saving || !hasChanges}
              style={{ background: "#fff", color: "#1a2b4c", border: "1px solid #d0d7e6" }}
            >
              Reset
            </button>
          </div>
        </div>

        <div className="settings-card">
          <h3>Platform Controls</h3>

          <div style={{ marginBottom: 16 }}>
            <strong>Status: </strong>
            <span
              style={{
                color: maintenanceMode ? "#dc2626" : "#16a34a",
                fontWeight: 700,
              }}
            >
              {maintenanceMode ? "Maintenance ON" : "System Live"}
            </span>
          </div>

          <div className="toggle-row">
            <span>Enable Notifications</span>
            <label className="switch">
              <input
                type="checkbox"
                checked={notificationsEnabled}
                onChange={() => setNotificationsEnabled(!notificationsEnabled)}
              />
              <span className="slider"></span>
            </label>
          </div>

          <div className="toggle-row">
            <span>Maintenance Mode</span>
            <label className="switch">
              <input
                type="checkbox"
                checked={maintenanceMode}
                onChange={() => setMaintenanceMode(!maintenanceMode)}
              />
              <span className="slider"></span>
            </label>
          </div>

          <label style={{ marginTop: 14, display: "block" }}>
            Maintenance Message
          </label>
          <textarea
            value={maintenanceMessage}
            onChange={(e) => setMaintenanceMessage(e.target.value)}
            rows={4}
            placeholder="Message users see during maintenance"
            style={{
              width: "100%",
              borderRadius: 8,
              padding: 10,
              marginTop: 8,
              border: "1px solid #d0d7e6",
              resize: "vertical",
            }}
          />

          <div style={{ display: "flex", gap: 10, flexWrap: "wrap", marginTop: 16 }}>
            <button className="danger-btn" onClick={saveSettings} disabled={saving}>
              {saving ? "Applying..." : "Apply Changes"}
            </button>

            <button
              className="danger-btn"
              onClick={toggleMaintenanceQuickly}
              disabled={saving}
              style={{
                background: maintenanceMode ? "#16a34a" : "#dc2626",
              }}
            >
              {maintenanceMode ? "Turn Maintenance OFF" : "Turn Maintenance ON"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}