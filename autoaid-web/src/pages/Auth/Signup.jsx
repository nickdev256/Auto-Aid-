import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import "./Signup.css";

export default function Signup() {
  const navigate = useNavigate();

  const [role, setRole] = useState(null);
  const [loading, setLoading] = useState(false);

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState("");

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
    phone: "",
    businessType: "",
    subscription: "monthly", // provider preferred plan
  });

  /* ===============================
     PASSWORD STRENGTH LOGIC
  =============================== */
  const evaluateStrength = (value) => {
    if (value.length < 4) return "Weak";
    if (value.length < 7) return "Medium";
    if (/[A-Z]/.test(value) && /\d/.test(value) && /[@$!%*?&#]/.test(value))
      return "Strong";
    return "Medium";
  };

  /* ===============================
     UGANDA PHONE VALIDATION
     Accepts: 776135485 (9 digits)
  =============================== */
  const validateUgandaPhone = (num) => {
    const cleaned = num.replace(/\D/g, "");
    if (!/^[0-9]{9}$/.test(cleaned)) return false;

    const prefix = cleaned.substring(0, 2);
    const allowedPrefixes = ["70", "74", "75", "76", "77", "78"];
    return allowedPrefixes.includes(prefix);
  };

  /* ===============================
     AUTO FORMAT PHONE → "776 135485"
  =============================== */
  const formatPhone = (value) => {
    let numbers = value.replace(/\D/g, "").slice(0, 9);
    if (numbers.length > 3) return numbers.slice(0, 3) + " " + numbers.slice(3);
    return numbers;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;

    if (name === "phone") {
      const formatted = formatPhone(value);
      setFormData({ ...formData, phone: formatted });
      return;
    }

    if (name === "password") {
      setPasswordStrength(evaluateStrength(value));
    }

    setFormData({ ...formData, [name]: value });
  };

  /* ===============================
     SUBMIT SIGNUP → SEND EMAIL OTP
     Flow: Signup → OTP → Subscription (provider)
  =============================== */
  const submit = async (e) => {
    e.preventDefault();

    if (!role) return alert("Choose User or Service Provider");

    if (!validateUgandaPhone(formData.phone))
      return alert("Invalid Uganda phone number");

    if (formData.password !== formData.confirmPassword)
      return alert("Passwords do not match!");

    // Provider must select service
    if (role === "provider" && !formData.businessType)
      return alert("Select the service you provide");

    setLoading(true);

    try {
      const cleaned = formData.phone.replace(/\D/g, "");
      const fullPhone = "+256" + cleaned;

      // ✅ Save email so OTP works even after refresh
      localStorage.setItem("pendingEmail", formData.email);

      const payload = {
        name: formData.name,
        email: formData.email,
        phone: fullPhone,
        password: formData.password,
        role,

        // backend expects these keys (safe defaults)
        businessName: role === "provider" ? formData.name : "",
        businessType: role === "provider" ? formData.businessType : "",
        servicesOffered: role === "provider" ? [formData.businessType] : [],

        // ✅ optional preferred plan (store it in OTP formData if you support it)
        subscriptionPlan: role === "provider" ? formData.subscription : null,
      };

      await axios.post("http://localhost:5001/api/auth/signup", payload, {
        withCredentials: true,
      });

      navigate("/otp", { state: { email: formData.email } });
    } catch (err) {
      console.log("SIGNUP ERROR status:", err.response?.status);
      console.log("SIGNUP ERROR data:", err.response?.data);
      alert(err.response?.data?.message || "Signup failed");
    } finally {
      setLoading(false);
    }
  };

  /* ===============================
     ROLE SELECTION
  =============================== */
  if (!role) {
    return (
      <div className="signup-bg">
        <div className="signup-right">
          <div className="role-select-card">
            <div className="auth-brand">
              <div className="auth-title">
                <h1>AutoAID</h1>
                <span>Emergency Services</span>
              </div>
            </div>

            <h2>Sign Up As</h2>

            <button className="role-btn" onClick={() => setRole("user")}>
              👤 User
            </button>

            <button className="role-btn" onClick={() => setRole("provider")}>
              🛠 Service Provider
            </button>

            <p className="signup-footer-text">
              Already have an account?{" "}
              <a onClick={() => navigate("/login")}>Login</a>
            </p>
          </div>
        </div>
      </div>
    );
  }

  /* ===============================
     MAIN SIGNUP UI
  =============================== */
  return (
    <div className="signup-bg">
      <div className="signup-left">
        <div className="left-content">
          <img src="/images/logo.jpg" className="left-logo" alt="AutoAID" />
          <h1>Welcome</h1>
          <h2>Please Sign Up</h2>
          <p>Fast help at your location.</p>
        </div>
      </div>

      <div className="signup-right">
        <div className="signup-card">
          <div className="auth-brand">
            <div className="auth-title">
              <h1>AutoAID</h1>
              <span>Emergency Services</span>
            </div>
          </div>

          <form className="signup-form" onSubmit={submit}>
            {/* NAME */}
            <div className="input-icon-wrapper">
              <span className="input-icon">👤</span>
              <input
                type="text"
                name="name"
                placeholder="Full Name"
                required
                value={formData.name}
                onChange={handleChange}
              />
            </div>

            {/* EMAIL + PHONE */}
            <div className="two-input-row">
              <div className="input-icon-wrapper">
                <span className="input-icon">📧</span>
                <input
                  type="email"
                  name="email"
                  placeholder="Email Address"
                  required
                  value={formData.email}
                  onChange={handleChange}
                />
              </div>

              <div className="input-icon-wrapper phone-field">
                <span className="input-icon">📞</span>
                <input
                  type="tel"
                  name="phone"
                  placeholder="776 123456"
                  required
                  value={formData.phone}
                  onChange={handleChange}
                />
              </div>
            </div>

            {/* PASSWORDS */}
            <div className="two-input-row">
              <div className="password-wrapper">
                <input
                  type={showPassword ? "text" : "password"}
                  name="password"
                  placeholder="Password"
                  required
                  value={formData.password}
                  onChange={handleChange}
                />
                <span
                  className="toggle-password"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? "🙈" : "👁️"}
                </span>
              </div>

              <div className="password-wrapper">
                <input
                  type={showConfirm ? "text" : "password"}
                  name="confirmPassword"
                  placeholder="Confirm Password"
                  required
                  value={formData.confirmPassword}
                  onChange={handleChange}
                />
                <span
                  className="toggle-password"
                  onClick={() => setShowConfirm(!showConfirm)}
                >
                  {showConfirm ? "🙈" : "👁️"}
                </span>
              </div>
            </div>

            {/* PASSWORD STRENGTH */}
            {formData.password && (
              <div className={`strength-bar ${passwordStrength.toLowerCase()}`}>
                {passwordStrength} Password
              </div>
            )}

            {/* PROVIDER FIELDS */}
            {role === "provider" && (
              <div className="two-input-row">
                <select
                  name="businessType"
                  required
                  value={formData.businessType}
                  onChange={handleChange}
                >
                  <option value="">Select Service</option>
                  <option value="garage">Garage / Mechanic</option>
                  <option value="fuel">Fuel Delivery</option>
                  <option value="towing">Towing</option>
                  <option value="ambulance">Ambulance</option>
                </select>

                <select
                  name="subscription"
                  required
                  value={formData.subscription}
                  onChange={handleChange}
                >
                  <option value="monthly">Monthly</option>
                  <option value="quarterly">Quarterly</option>
                  <option value="yearly">Yearly</option>
                </select>
              </div>
            )}

            <button type="submit" disabled={loading}>
              {loading ? "Processing..." : "Continue"}
            </button>
          </form>

          <p className="signup-footer-text">
            Already have an account?{" "}
            <a onClick={() => navigate("/login")}>Login</a>
          </p>
        </div>
      </div>
    </div>
  );
}