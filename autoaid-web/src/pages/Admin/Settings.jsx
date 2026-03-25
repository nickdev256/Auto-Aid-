import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import "./Settings.css";

const API_BASE =
  import.meta.env.VITE_API_URL?.replace(/\/$/, "") || "http://localhost:5001";

const DEFAULT_MAINTENANCE_MESSAGE =
  "AutoAid is currently under scheduled maintenance. Some services may be temporarily unavailable. Please try again later.";

const DEFAULT_EMAIL_TEMPLATE = {
  subject: "",
  heading: "",
  message: "",
  audience: "all",
  sendEmail: true,
  sendNotification: false,
};

export default function Settings() {
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [sendingEmail, setSendingEmail] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [lastSavedAt, setLastSavedAt] = useState(null);

  const [settings, setSettings] = useState({
    systemName: "AutoAid",
    supportEmail: "",
    supportPhone: "",
    whatsappNumber: "",
    notificationsEnabled: true,
    maintenanceMode: false,
    maintenanceMessage: DEFAULT_MAINTENANCE_MESSAGE,
    maintenanceTarget: "both", // web | android | both
    allowUserRegistration: true,
    allowProviderRegistration: true,
    autoApproveProviders: false,
    emergencyHotline: "",
  });

  const [initialSettings, setInitialSettings] = useState(null);

  const [emailForm, setEmailForm] = useState(DEFAULT_EMAIL_TEMPLATE);
  const [emailSuccess, setEmailSuccess] = useState("");
  const [emailError, setEmailError] = useState("");

  const fetchSettings = useCallback(async () => {
    setLoading(true);
    setError("");
    setSuccess("");

    try {
      const res = await fetch(`${API_BASE}/api/admin/settings`, {
        method: "GET",
        credentials: "include",
      });

      if (res.status === 401) {
        throw new Error("Session expired. Please login again.");
      }

      if (!res.ok) {
        const msg = await res.text().catch(() => "Failed to load settings");
        throw new Error(msg);
      }

      const data = await res.json();

      const nextSettings = {
        systemName: data?.systemName || "AutoAid",
        supportEmail: data?.supportEmail || "",
        supportPhone: data?.supportPhone || "",
        whatsappNumber: data?.whatsappNumber || "",
        notificationsEnabled:
          typeof data?.notificationsEnabled === "boolean"
            ? data.notificationsEnabled
            : true,
        maintenanceMode:
          typeof data?.maintenanceMode === "boolean"
            ? data.maintenanceMode
            : false,
        maintenanceMessage:
          data?.maintenanceMessage || DEFAULT_MAINTENANCE_MESSAGE,
        maintenanceTarget: data?.maintenanceTarget || "both",
        allowUserRegistration:
          typeof data?.allowUserRegistration === "boolean"
            ? data.allowUserRegistration
            : true,
        allowProviderRegistration:
          typeof data?.allowProviderRegistration === "boolean"
            ? data.allowProviderRegistration
            : true,
        autoApproveProviders:
          typeof data?.autoApproveProviders === "boolean"
            ? data.autoApproveProviders
            : false,
        emergencyHotline: data?.emergencyHotline || "",
      };

      setSettings(nextSettings);
      setInitialSettings(nextSettings);
      setLastSavedAt(new Date());
    } catch (err) {
      console.error("Settings load error:", err);
      setError(err.message || "Failed to load settings");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  const hasChanges = useMemo(() => {
    if (!initialSettings) return false;
    return JSON.stringify(settings) !== JSON.stringify(initialSettings);
  }, [settings, initialSettings]);

  const systemStatus = settings.maintenanceMode ? "Maintenance" : "Live";

  const updateSetting = (key, value) => {
    setSettings((prev) => ({
      ...prev,
      [key]: value,
    }));
    setSuccess("");
    setError("");
  };

  const validateEmail = (email) => {
    if (!email) return false;
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  };

  const saveSettings = async () => {
    setSaving(true);
    setError("");
    setSuccess("");

    if (settings.supportEmail && !validateEmail(settings.supportEmail)) {
      setSaving(false);
      setError("Please enter a valid support email address.");
      return;
    }

    if (
      settings.maintenanceMode &&
      !String(settings.maintenanceMessage || "").trim()
    ) {
      setSaving(false);
      setError("Maintenance message is required when maintenance mode is on.");
      return;
    }

    try {
      const res = await fetch(`${API_BASE}/api/admin/settings`, {
        method: "PUT",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(settings),
      });

      if (res.status === 401) {
        throw new Error("Session expired. Please login again.");
      }

      if (!res.ok) {
        const msg = await res.text().catch(() => "Failed to save settings");
        throw new Error(msg);
      }

      const saved = await res.json().catch(() => settings);
      const normalized = {
        ...settings,
        ...saved,
      };

      setSettings(normalized);
      setInitialSettings(normalized);
      setLastSavedAt(new Date());
      setSuccess("Settings saved successfully.");
    } catch (err) {
      console.error("Settings save error:", err);
      setError(err.message || "Failed to save settings");
    } finally {
      setSaving(false);
    }
  };

  const resetChanges = () => {
    if (!initialSettings) return;
    setSettings(initialSettings);
    setError("");
    setSuccess("");
  };

  const restoreDefaultMaintenanceMessage = () => {
    updateSetting("maintenanceMessage", DEFAULT_MAINTENANCE_MESSAGE);
  };

  const sendMarketingEmail = async () => {
    setSendingEmail(true);
    setEmailSuccess("");
    setEmailError("");

    if (!emailForm.subject.trim()) {
      setSendingEmail(false);
      setEmailError("Email subject is required.");
      return;
    }

    if (!emailForm.message.trim()) {
      setSendingEmail(false);
      setEmailError("Email message is required.");
      return;
    }

    try {
      const res = await fetch(`${API_BASE}/api/admin/marketing-email/send`, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(emailForm),
      });

      if (res.status === 401) {
        throw new Error("Session expired. Please login again.");
      }

      if (!res.ok) {
        const msg = await res.text().catch(() => "Failed to send email");
        throw new Error(msg);
      }

      const result = await res.json().catch(() => ({}));

      setEmailSuccess(
        result?.message || "Marketing email / communication sent successfully."
      );
      setEmailForm(DEFAULT_EMAIL_TEMPLATE);
    } catch (err) {
      console.error("Email send error:", err);
      setEmailError(
        err.message ||
          "Failed to send marketing communication. Add the backend endpoint first if it does not exist."
      );
    } finally {
      setSendingEmail(false);
    }
  };

  if (loading) {
    return (
      <div className="settings-page">
        <main className="settings-container">
          <div className="loading-box">Loading settings...</div>
        </main>
      </div>
    );
  }

  return (
    <div className="settings-page">
      <main className="settings-container">
        <section className="settings-hero card-ui">
          <div>
            <div className="hero-badge">Admin / Settings</div>
            <h1>System Settings</h1>
            <p>
              Manage platform identity, maintenance mode, registration controls,
              communication preferences, and admin email campaigns.
            </p>
          </div>

          <div className="hero-actions">
            <button
              className="btn btn-light"
              type="button"
              onClick={() => navigate("/admin")}
            >
              Back to Dashboard
            </button>

            <button
              className="btn btn-light"
              type="button"
              onClick={fetchSettings}
            >
              Refresh
            </button>

            <button
              className="btn btn-primary"
              type="button"
              onClick={saveSettings}
              disabled={saving || !hasChanges}
            >
              {saving ? "Saving..." : hasChanges ? "Save Changes" : "Saved"}
            </button>
          </div>
        </section>

        <section className="settings-stats">
          <div className="stat-box">
            <span>System Status</span>
            <strong>{systemStatus}</strong>
          </div>
          <div className="stat-box">
            <span>Notifications</span>
            <strong>{settings.notificationsEnabled ? "Enabled" : "Disabled"}</strong>
          </div>
          <div className="stat-box">
            <span>Unsaved Changes</span>
            <strong>{hasChanges ? "Yes" : "No"}</strong>
          </div>
          <div className="stat-box">
            <span>Maintenance Target</span>
            <strong>
              {settings.maintenanceTarget === "both"
                ? "Web + Android"
                : settings.maintenanceTarget === "web"
                ? "Web Only"
                : "Android Only"}
            </strong>
          </div>
        </section>

        {error && <div className="alert-box error">{error}</div>}
        {success && <div className="alert-box success">{success}</div>}

        <section className="settings-grid">
          <div className="card-ui section-card">
            <div className="section-head">
              <h3>System Information</h3>
              <p>Core platform identity and support contacts.</p>
            </div>

            <div className="form-grid">
              <div className="field">
                <label>System Name</label>
                <input
                  type="text"
                  value={settings.systemName}
                  onChange={(e) => updateSetting("systemName", e.target.value)}
                  placeholder="AutoAid"
                />
              </div>

              <div className="field">
                <label>Support Email</label>
                <input
                  type="email"
                  value={settings.supportEmail}
                  onChange={(e) => updateSetting("supportEmail", e.target.value)}
                  placeholder="support@autoaid.com"
                />
              </div>

              <div className="field">
                <label>Support Phone</label>
                <input
                  type="text"
                  value={settings.supportPhone}
                  onChange={(e) => updateSetting("supportPhone", e.target.value)}
                  placeholder="+256..."
                />
              </div>

              <div className="field">
                <label>WhatsApp Number</label>
                <input
                  type="text"
                  value={settings.whatsappNumber}
                  onChange={(e) => updateSetting("whatsappNumber", e.target.value)}
                  placeholder="+256..."
                />
              </div>

              <div className="field full">
                <label>Emergency Hotline</label>
                <input
                  type="text"
                  value={settings.emergencyHotline}
                  onChange={(e) =>
                    updateSetting("emergencyHotline", e.target.value)
                  }
                  placeholder="Emergency hotline number"
                />
              </div>
            </div>
          </div>

          <div className="card-ui section-card">
            <div className="section-head">
              <h3>Platform Controls</h3>
              <p>Switch features on or off for the whole system.</p>
            </div>

            <div className="toggle-list">
              <div className="toggle-row">
                <div>
                  <strong>Enable Notifications</strong>
                  <span>Allow platform-wide admin notifications.</span>
                </div>
                <label className="switch">
                  <input
                    type="checkbox"
                    checked={settings.notificationsEnabled}
                    onChange={(e) =>
                      updateSetting("notificationsEnabled", e.target.checked)
                    }
                  />
                  <span className="slider" />
                </label>
              </div>

              <div className="toggle-row">
                <div>
                  <strong>Allow User Registration</strong>
                  <span>Let new users create accounts.</span>
                </div>
                <label className="switch">
                  <input
                    type="checkbox"
                    checked={settings.allowUserRegistration}
                    onChange={(e) =>
                      updateSetting("allowUserRegistration", e.target.checked)
                    }
                  />
                  <span className="slider" />
                </label>
              </div>

              <div className="toggle-row">
                <div>
                  <strong>Allow Provider Registration</strong>
                  <span>Let new service providers sign up.</span>
                </div>
                <label className="switch">
                  <input
                    type="checkbox"
                    checked={settings.allowProviderRegistration}
                    onChange={(e) =>
                      updateSetting("allowProviderRegistration", e.target.checked)
                    }
                  />
                  <span className="slider" />
                </label>
              </div>

              <div className="toggle-row">
                <div>
                  <strong>Auto Approve Providers</strong>
                  <span>
                    Automatically approve provider accounts after registration.
                  </span>
                </div>
                <label className="switch">
                  <input
                    type="checkbox"
                    checked={settings.autoApproveProviders}
                    onChange={(e) =>
                      updateSetting("autoApproveProviders", e.target.checked)
                    }
                  />
                  <span className="slider" />
                </label>
              </div>
            </div>
          </div>
        </section>

        <section className="settings-grid">
          <div className="card-ui section-card">
            <div className="section-head">
              <h3>Maintenance Mode</h3>
              <p>Control system availability for users and providers.</p>
            </div>

            <div className="toggle-row maintenance-main-row">
              <div>
                <strong>Enable Maintenance Mode</strong>
                <span>
                  Turn this on to temporarily limit access to the platform.
                </span>
              </div>

              <label className="switch">
                <input
                  type="checkbox"
                  checked={settings.maintenanceMode}
                  onChange={(e) =>
                    updateSetting("maintenanceMode", e.target.checked)
                  }
                />
                <span className="slider" />
              </label>
            </div>

            <div className="maintenance-warning">
              <strong>Important:</strong> Admin access and login can remain
              available depending on your backend logic. Web and Android users
              may see the maintenance message below.
            </div>

            <div className="form-grid">
              <div className="field">
                <label>Maintenance Target</label>
                <select
                  value={settings.maintenanceTarget}
                  onChange={(e) =>
                    updateSetting("maintenanceTarget", e.target.value)
                  }
                >
                  <option value="both">Web + Android</option>
                  <option value="web">Web Only</option>
                  <option value="android">Android Only</option>
                </select>
              </div>

              <div className="field full">
                <div className="label-row">
                  <label>Maintenance Message</label>
                  <span>
                    {String(settings.maintenanceMessage || "").length}/250
                  </span>
                </div>
                <textarea
                  rows={5}
                  maxLength={250}
                  value={settings.maintenanceMessage}
                  onChange={(e) =>
                    updateSetting("maintenanceMessage", e.target.value)
                  }
                  placeholder="Enter maintenance message..."
                />
              </div>
            </div>

            <div className="inline-actions">
              <button
                className="btn btn-light"
                type="button"
                onClick={restoreDefaultMaintenanceMessage}
              >
                Restore Default Message
              </button>
            </div>
          </div>

          <div className="card-ui section-card">
            <div className="section-head">
              <h3>Message Preview</h3>
              <p>What users will see during maintenance.</p>
            </div>

            <div className="preview-box">
              <div className="preview-badge">
                {settings.maintenanceMode ? "MAINTENANCE LIVE" : "PREVIEW"}
              </div>
              <h4>{settings.systemName || "AutoAid"}</h4>
              <p>
                {settings.maintenanceMessage || DEFAULT_MAINTENANCE_MESSAGE}
              </p>
              <span className="preview-target">
                Target:{" "}
                {settings.maintenanceTarget === "both"
                  ? "Web + Android"
                  : settings.maintenanceTarget === "web"
                  ? "Web Only"
                  : "Android Only"}
              </span>
            </div>

            <div className="status-note">
              Last saved:{" "}
              <strong>
                {lastSavedAt ? lastSavedAt.toLocaleString() : "Not yet saved"}
              </strong>
            </div>
          </div>
        </section>

        <section className="card-ui section-card">
          <div className="section-head">
            <h3>Email Marketing & Communication</h3>
            <p>
              Send announcements, offers, updates, and important communication
              from admin to users.
            </p>
          </div>

          {emailError && <div className="alert-box error">{emailError}</div>}
          {emailSuccess && <div className="alert-box success">{emailSuccess}</div>}

          <div className="form-grid">
            <div className="field">
              <label>Audience</label>
              <select
                value={emailForm.audience}
                onChange={(e) =>
                  setEmailForm((prev) => ({
                    ...prev,
                    audience: e.target.value,
                  }))
                }
              >
                <option value="all">All Users</option>
                <option value="providers">Providers Only</option>
                <option value="customers">Customers Only</option>
                <option value="verified_providers">Verified Providers</option>
              </select>
            </div>

            <div className="field">
              <label>Email Subject</label>
              <input
                type="text"
                value={emailForm.subject}
                onChange={(e) =>
                  setEmailForm((prev) => ({
                    ...prev,
                    subject: e.target.value,
                  }))
                }
                placeholder="Enter campaign subject"
              />
            </div>

            <div className="field full">
              <label>Email Heading</label>
              <input
                type="text"
                value={emailForm.heading}
                onChange={(e) =>
                  setEmailForm((prev) => ({
                    ...prev,
                    heading: e.target.value,
                  }))
                }
                placeholder="Enter email heading"
              />
            </div>

            <div className="field full">
              <div className="label-row">
                <label>Message Body</label>
                <span>{String(emailForm.message || "").length}/1000</span>
              </div>
              <textarea
                rows={7}
                maxLength={1000}
                value={emailForm.message}
                onChange={(e) =>
                  setEmailForm((prev) => ({
                    ...prev,
                    message: e.target.value,
                  }))
                }
                placeholder="Write your message to users..."
              />
            </div>
          </div>

          <div className="toggle-list compact">
            <div className="toggle-row">
              <div>
                <strong>Send Email</strong>
                <span>Deliver this message by email.</span>
              </div>
              <label className="switch">
                <input
                  type="checkbox"
                  checked={emailForm.sendEmail}
                  onChange={(e) =>
                    setEmailForm((prev) => ({
                      ...prev,
                      sendEmail: e.target.checked,
                    }))
                  }
                />
                <span className="slider" />
              </label>
            </div>

            <div className="toggle-row">
              <div>
                <strong>Send In-App Notification</strong>
                <span>Also push the message as an internal platform notification.</span>
              </div>
              <label className="switch">
                <input
                  type="checkbox"
                  checked={emailForm.sendNotification}
                  onChange={(e) =>
                    setEmailForm((prev) => ({
                      ...prev,
                      sendNotification: e.target.checked,
                    }))
                  }
                />
                <span className="slider" />
              </label>
            </div>
          </div>

          <div className="email-preview">
            <div className="email-preview-head">
              <span>Email Preview</span>
            </div>
            <div className="email-preview-body">
              <h4>{emailForm.heading || "Your heading will appear here"}</h4>
              <h5>{emailForm.subject || "Your email subject will appear here"}</h5>
              <p>{emailForm.message || "Your message preview will appear here."}</p>
            </div>
          </div>

          <div className="inline-actions">
            <button
              className="btn btn-light"
              type="button"
              onClick={() => setEmailForm(DEFAULT_EMAIL_TEMPLATE)}
            >
              Reset Email Form
            </button>

            <button
              className="btn btn-primary"
              type="button"
              onClick={sendMarketingEmail}
              disabled={sendingEmail}
            >
              {sendingEmail ? "Sending..." : "Send Communication"}
            </button>
          </div>
        </section>

        <section className="card-ui section-card danger-card">
          <div className="section-head">
            <h3>Danger Zone</h3>
            <p>Use these carefully. These actions affect the whole platform.</p>
          </div>

          <div className="danger-box">
            <strong>Emergency Maintenance</strong>
            <p>
              If the platform is unstable, enable maintenance mode immediately,
              save changes, and set a clear user-facing message.
            </p>

            <div className="inline-actions">
              <button
                className="btn btn-danger"
                type="button"
                onClick={() => {
                  updateSetting("maintenanceMode", true);
                  if (!settings.maintenanceMessage?.trim()) {
                    updateSetting(
                      "maintenanceMessage",
                      DEFAULT_MAINTENANCE_MESSAGE
                    );
                  }
                }}
              >
                Turn On Emergency Maintenance
              </button>

              <button
                className="btn btn-light"
                type="button"
                onClick={resetChanges}
              >
                Reset Unsaved Changes
              </button>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}