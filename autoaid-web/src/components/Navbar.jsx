import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import "./Navbar.css";

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 30);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <header className={`nav-wrap ${scrolled ? "scrolled" : ""}`}>
      <nav className="navbar">

        {/* LOGO LEFT */}
        <Link to="/" className="nav-left">
          <img src="/images/logo2.jpg" alt="AutoAID" className="nav-logo" />
          <span className="brand">AutoAID</span>
        </Link>

        {/* MOBILE TOGGLE */}
        <button
          className={`nav-toggle ${open ? "open" : ""}`}
          aria-label="toggle menu"
          onClick={() => setOpen((prev) => !prev)}
        >
          <span />
          <span />
          <span />
        </button>

        {/* NAV LINKS */}
        <div className={`nav-links ${open ? "open" : ""}`}>
          <Link to="/" onClick={() => setOpen(false)}>Home</Link>

          {/* PAGE SECTION LINKS */}
          <a href="#services" onClick={() => setOpen(false)}>Services</a>
          <a href="#why" onClick={() => setOpen(false)}>Why</a>
          <a href="#how" onClick={() => setOpen(false)}>How It Works</a>
          <a href="#contact" onClick={() => setOpen(false)}>Contact</a>

          {/* CTA BUTTON */}
          <Link
            to="/signup"
            className="btn-login"
            onClick={() => setOpen(false)}
          >
            Sign Up
          </Link>
        </div>

      </nav>
    </header>
  );
}
