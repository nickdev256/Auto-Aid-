import React, { useState, useRef, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import axios from "axios";
import "./OTPVerify.css";
import { useAuth } from "../../context/AuthContext";

const API = import.meta.env.VITE_API_URL
  ? `${import.meta.env.VITE_API_URL}/api/auth`
  : "http://localhost:5001/api/auth";

export default function OTPVerify() {
  const { state } = useLocation();
  const navigate = useNavigate();
  const { setUser } = useAuth();

  // ✅ refresh-safe email
  const email = state?.email || localStorage.getItem("pendingEmail");

  const [otp, setOtp] = useState(["", "", "", "", "", ""]);
  const [loading, setLoading] = useState(false);
  const [shake, setShake] = useState(false);
  const [success, setSuccess] = useState(false);

  const [timer, setTimer] = useState(60);
  const [canResend, setCanResend] = useState(false);

  const inputRefs = useRef([]);
  const autoSubmittingRef = useRef(false);

  useEffect(() => {
    if (!email) navigate("/signup", { replace: true });
  }, [email, navigate]);

  // focus first box
  useEffect(() => {
    if (email) {
      setTimeout(() => inputRefs.current[0]?.focus(), 150);
    }
  }, [email]);

  /* ================================
       COUNTDOWN TIMER
  =================================*/
  useEffect(() => {
    if (timer === 0) {
      setCanResend(true);
      return;
    }
    const countdown = setTimeout(() => setTimer((t) => t - 1), 1000);
    return () => clearTimeout(countdown);
  }, [timer]);

  const resendOTP = async () => {
    try {
      setCanResend(false);
      setTimer(60);

      await axios.post(
        `${API}/resend-otp`,
        { email },
        { withCredentials: true }
      );

      alert("New OTP sent!");
    } catch (err) {
      console.log("RESEND ERROR status:", err.response?.status);
      console.log("RESEND ERROR data:", err.response?.data);
      alert(err.response?.data?.message || "Failed to resend OTP.");
    }
  };

  /* ================================
       HANDLE INPUT CHANGE
  =================================*/
  const handleChange = (value, index) => {
    if (!/^[0-9]?$/.test(value)) return;

    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);

    // move to next
    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }

    // auto submit when all 6 are filled
    const joined = newOtp.join("");
    if (joined.length === 6 && !joined.includes("") && !autoSubmittingRef.current) {
      autoSubmittingRef.current = true;
      setTimeout(() => submit(null, joined), 120);
    }
  };

  const handleKeyDown = (e, index) => {
    if (e.key === "Backspace" && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const clearOtpInputs = () => {
    setOtp(["", "", "", "", "", ""]);
    autoSubmittingRef.current = false;
    setTimeout(() => inputRefs.current[0]?.focus(), 150);
  };

  /* ================================
       SUBMIT OTP
  =================================*/
  const submit = async (e, autoOTP = null) => {
    if (e) e.preventDefault();

    const finalOTP = (autoOTP || otp.join("")).replace(/\D/g, "");
    if (finalOTP.length !== 6) return;

    setLoading(true);

    try {
      const res = await axios.post(
        `${API}/verify-otp`,
        { email, otp: finalOTP },
        { withCredentials: true }
      );

      const user = res.data?.user;

      // ✅ clear OTP temporary email
      localStorage.removeItem("pendingEmail");

      // ✅ PROVIDER FLOW:
      // Do NOT setUser here. Provider must subscribe then login.
      if (user?.role === "provider") {
        // store providerId for subscription page
        localStorage.setItem("pendingProviderId", user._id || user.id);
        localStorage.setItem("pendingProviderEmail", user.email || email);

        setSuccess(true);

        setTimeout(() => {
          navigate("/provider/subscription", { replace: true });
        }, 900);

        return;
      }

      // ✅ NORMAL USER FLOW: can proceed to login (or auto-login if you want)
      if (user) setUser(user);

      setSuccess(true);

      setTimeout(() => {
        navigate("/login", { replace: true });
      }, 900);
    } catch (err) {
      console.log("VERIFY ERROR message:", err.message);
      console.log("VERIFY ERROR status:", err.response?.status);
      console.log("VERIFY ERROR data:", err.response?.data);

      setShake(true);
      setTimeout(() => setShake(false), 600);

      const msg = err.response?.data?.message;

      if (msg === "OTP expired") {
        alert("OTP expired. Please resend a new OTP.");
      } else if (msg === "Incorrect OTP") {
        alert("Incorrect OTP. Try again.");
        clearOtpInputs();
      } else if (msg === "OTP not found") {
        alert("OTP not found. Please signup again.");
        navigate("/signup", { replace: true });
      } else {
        alert(msg || "OTP verification failed.");
      }

      autoSubmittingRef.current = false;
    } finally {
      setLoading(false);
    }
  };

  if (!email) return null;

  return (
    <div className="otp-page">
      <div className={`otp-card ${shake ? "shake" : ""} ${success ? "success" : ""}`}>
        <h2>Email Verification</h2>
        <p>Enter the 6-digit code sent to:</p>
        <p className="otp-email">{email}</p>

        <form onSubmit={submit}>
          <div className="otp-input-group">
            {otp.map((digit, index) => (
              <input
                key={index}
                ref={(el) => (inputRefs.current[index] = el)}
                type="text"
                inputMode="numeric"
                maxLength="1"
                value={digit}
                onChange={(e) => handleChange(e.target.value, index)}
                onKeyDown={(e) => handleKeyDown(e, index)}
                className="otp-box"
              />
            ))}
          </div>

          <button
            className="otp-btn"
            type="submit"
            disabled={loading || otp.join("").length !== 6}
          >
            {loading ? "Verifying..." : "Verify OTP"}
          </button>
        </form>

        <div className="resend-wrapper">
          {!canResend ? (
            <p className="resend-timer">Resend OTP in {timer}s</p>
          ) : (
            <button className="resend-btn" onClick={resendOTP}>
              Resend OTP
            </button>
          )}
        </div>
      </div>
    </div>
  );
}