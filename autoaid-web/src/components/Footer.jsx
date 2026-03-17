import React from "react";
import "./Footer.css";
import { FaFacebookF, FaTwitter, FaInstagram, FaYoutube, FaWhatsapp } from "react-icons/fa";

export default function Footer() {
  return (
    <footer className="footer">

      <div className="footer-inner">

        {/* BRAND */}
        <div className="footer-section brand-section">
          <img src="/images/logo2.jpg" alt="AutoAID" className="footer-logo" />
          <h3 className="brand-name">AutoAID</h3>
          <p className="brand-desc">
            Reliable emergency vehicle support across Uganda.
          </p>

          {/* SOCIAL ICONS */}
          <div className="social-icons">
            <a href="#" className="social"><FaFacebookF /></a>
            <a href="#" className="social"><FaTwitter /></a>
            <a href="#" className="social"><FaInstagram /></a>
            <a href="#" className="social"><FaYoutube /></a>
          </div>
        </div>

        {/* QUICK LINKS */}
        <div className="footer-section">
          <h4>Quick Links</h4>
          <ul>
            <li><a href="#services">Services</a></li>
            <li><a href="#why">Why Choose Us</a></li>
            <li><a href="#how">How It Works</a></li>
            <li><a href="#contact">Contact</a></li>
            <li><a href="/signup">Join AutoAID</a></li>
          </ul>
        </div>

        {/* CONTACT INFO */}
        <div className="footer-section">
          <h4>Contact Us</h4>
          <p>📍 Kampala, Uganda</p>
          <p>📞 +256 776135485</p>
          <p>✉ support@autoaid.com</p>
        </div>

        {/* NEWSLETTER */}
        <div className="footer-section newsletter">
          <h4>Stay Updated</h4>
          <p>Get alerts, feature updates & safety tips.</p>

          <div className="newsletter-box">
            <input type="email" placeholder="Enter your email" />
            <button>Subscribe</button>
          </div>
        </div>

      </div>

      {/* COPYRIGHT */}
      <div className="footer-bottom">
        © {new Date().getFullYear()} AutoAID. All rights reserved.
      </div>

     
      <a href="#" className="back-to-top">↑</a>

      
      <a
        href="https://wa.me/256776135485"
        target="_blank"
        rel="noopener noreferrer"
        className="whatsapp-btn"
      >
        <FaWhatsapp />
      </a>

    </footer>
  );
}
