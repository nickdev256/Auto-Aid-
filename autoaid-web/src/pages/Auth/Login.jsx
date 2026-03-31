import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "./Login.css";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [role, setRole] = useState(null);
  const [form, setForm] = useState({ email: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");

  const handleChange = (e) => {
    setForm((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const goToProviderDashboard = (user) => {
    const type = (user?.businessType || "").toLowerCase().trim();

    if (type === "garage") {
      navigate("/provider/garage");
      return;
    }

    if (type === "fuel") {
      navigate("/provider/fuel");
      return;
    }

    if (type === "towing") {
      navigate("/provider/towing");
      return;
    }

    if (type === "ambulance") {
      navigate("/provider/ambulance");
      return;
    }

    // fallback if businessType is missing or unknown
    navigate("/provider/dashboard");
  };

  const submit = async (e) => {
    e.preventDefault();

    if (!role) {
      alert("Please choose account type");
      return;
    }

    setErrorMsg("");
    setLoading(true);

    try {
      const user = await login(form.email, form.password);

      // ADMIN LOGIN
      if (user?.role === "admin") {
        navigate("/admin");
        return;
      }

      // PROVIDER LOGIN
      if (user?.role === "provider") {
        if (role !== "provider") {
          setErrorMsg("This is a provider account. Select Provider login.");
          return;
        }

        const status = (user?.status || "").toLowerCase().trim();

        if (status === "pending") {
          navigate("/provider/pending");
          return;
        }

        if (status === "approved") {
          goToProviderDashboard(user);
          return;
        }

        navigate("/provider/rejected");
        return;
      }

      // NORMAL USER LOGIN
      if (role === "user" && user?.role === "user") {
        navigate("/dashboard");
        return;
      }

      setErrorMsg("Role does not match this account.");
    } catch (err) {
      setErrorMsg(err?.message || "Login failed");
    } finally {
      setLoading(false);
    }
  };

  if (!role) {
    return (
      <div className="login-bg">
        <div className="role-select-card">
          <div className="auth-brand">
            <img src="/images/logo.jpg" className="auth-logo" alt="logo" />
            <div className="auth-title">
              <h1>AutoAID</h1>
              <span>Emergency Services</span>
            </div>
          </div>

          <h2>Login As</h2>

          <button className="role-btn" onClick={() => setRole("user")}>
            👤 User Login
          </button>

          <button className="role-btn" onClick={() => setRole("provider")}>
            🛠 Provider Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="login-bg">
      <div className="login-card">
        <div className="auth-brand">
          <img src="/images/logo.jpg" className="auth-logo" alt="logo" />
          <div className="auth-title">
            <h1>AutoAID</h1>
            <span>Emergency Services</span>
          </div>
        </div>

        <h2>{role === "provider" ? "Provider Login" : "User Login"}</h2>
        <p className="tagline">Welcome back, let's get you moving.</p>

        <form onSubmit={submit} className="login-form">
          {errorMsg && <p className="error">{errorMsg}</p>}

          <input
            type="email"
            name="email"
            placeholder="Email Address"
            value={form.email}
            onChange={handleChange}
            required
          />

          <input
            type="password"
            name="password"
            placeholder="Password"
            value={form.password}
            onChange={handleChange}
            required
          />

          <button type="submit" disabled={loading}>
            {loading ? "Processing..." : "Login"}
          </button>
        </form>

        <div className="login-footer-text">
          <a href="/reset">Forgot Password?</a>
          <br />
          Don’t have an account?{" "}
          <a
            href="#signup"
            onClick={(e) => {
              e.preventDefault();
              navigate("/signup");
            }}
          >
            Create one
          </a>
        </div>
      </div>
    </div>
  );
}