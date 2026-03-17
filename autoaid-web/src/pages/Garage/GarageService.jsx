import React from "react";
import { useNavigate } from "react-router-dom";
import { FaTools, FaCarBattery, FaCarCrash, FaWrench, FaLocationArrow } from "react-icons/fa";
import "./GarageService.css";

export default function GarageService() {
  const nav = useNavigate();

  return (
    <div className="garage-service-wrapper">

      {/* BACK + HERO */}
      <div className="garage-toprow">
        <button className="back-to-dash" onClick={() => nav("/dashboard")}>
          ← Back to Dashboard
        </button>
      </div>

      {/* ================= HERO ================= */}
      <section className="garage-hero">
        <div className="garage-hero-content">
          <h1>Garage Support</h1>
          <p>Your trusted emergency breakdown & repair partner.</p>

          <button
            className="hero-request-btn"
            onClick={() => nav("/garage/request")}
          >
            Request Mechanic Now
          </button>
        </div>
      </section>

      {/* ================= FEATURES ================= */}
      <section className="garage-features">
        <h2>What We Offer</h2>

        <div className="garage-grid">
          <div className="g-card">
            <FaTools className="g-icon" />
            <h4>Mechanical Diagnosis</h4>
            <p>Instant assessment of breakdowns and engine issues.</p>
          </div>

          <div className="g-card">
            <FaCarBattery className="g-icon" />
            <h4>Battery Jump-start</h4>
            <p>Dead battery? We help you get moving instantly.</p>
          </div>

          <div className="g-card">
            <FaWrench className="g-icon" />
            <h4>Tyre Replacement</h4>
            <p>Flat tyre replacement or on-spot quick fixing.</p>
          </div>

          <div className="g-card">
            <FaCarCrash className="g-icon" />
            <h4>Minor Repairs</h4>
            <p>Small breakdowns fixed on-site by certified mechanics.</p>
          </div>
        </div>
      </section>

      {/* ================= HOW IT WORKS ================= */}
      <section className="garage-how">
        <h2>How It Works</h2>

        <div className="how-steps">
          <div className="how-step">
            <span className="step-circle">01</span>
            <h4>Tell Us Your Issue</h4>
            <p>Describe what happened — engine, tyre, battery, etc.</p>
          </div>

          <div className="how-step">
            <span className="step-circle">02</span>
            <h4>Nearest Garage Assigned</h4>
            <p>We locate the closest available mechanic instantly.</p>
          </div>

          <div className="how-step">
            <span className="step-circle">03</span>
            <h4>Mechanic Accepts Request</h4>
            <p>A verified mechanic accepts and prepares help.</p>
          </div>

          <div className="how-step">
            <span className="step-circle">04</span>
            <h4>Track Mechanic Live</h4>
            <p>Monitor your mechanic’s movement in real time.</p>
          </div>
        </div>
      </section>

      {/* ================= CTA CARD ================= */}
      <section className="garage-cta">
        <div className="cta-box">
          <h2>Need Emergency Help Now?</h2>
          <p>Our certified mechanics are available 24/7 across Uganda.</p>

          <button className="cta-btn" onClick={() => nav("/garage/request")}>
            Request Garage Assistance
          </button>
        </div>
      </section>

    </div>
  );
}
