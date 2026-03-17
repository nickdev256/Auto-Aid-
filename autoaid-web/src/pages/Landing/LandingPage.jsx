import React, { useEffect, useState } from "react";
import "./LandingPage.css";

export default function LandingPage() {

  /* ============================================
       SCROLL ANIMATION
  ============================================ */
  useEffect(() => {
    const elements = document.querySelectorAll("[data-animate]");

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => entry.isIntersecting && entry.target.classList.add("visible"));
      },
      { threshold: 0.2 }
    );

    elements.forEach((el) => observer.observe(el));
  }, []);


  /* ============================================
       MODAL STATE
  ============================================ */
  const [modalData, setModalData] = useState(null);

  const openModal = (service) => setModalData(service);
  const closeModal = () => setModalData(null);


  /* ============================================
       SERVICE INFORMATION (MODAL)
  ============================================ */
  const SERVICES = {
    garage: {
      title: "Garage Support",
      image: "/images/bg 1.jpg",
      desc: "On-site mechanical repairs, tire fixing, battery replacement & diagnostic support anywhere in Uganda."
    },
    fuel: {
      title: "Fuel Delivery",
      image: "/images/fuel.jpg",
      desc: "Petrol or diesel delivered instantly to your breakdown location — safe, fast, and reliable."
    },
    towing: {
      title: "Towing Service",
      image: "/images/towing.jpg",
      desc: "Emergency breakdown towing, accident recovery & long-distance transportation for all vehicle types."
    },
    ambulance: {
      title: "Ambulance Service",
      image: "/images/ambulance.jpg",
      desc: "Immediate medical assistance with trained paramedics and emergency transport."
    }
  };


  return (
    <div className="landing-wrapper">

      {/* ============================================
          HERO SECTION
      ============================================ */}
      <section className="wfmc-hero">
        <div className="wfmc-overlay"></div>

        <div className="wfmc-content" data-animate>
          <h1 className="wfmc-title">A Roadside Assistance Platform & Emergency Support in Uganda</h1>
          <p className="wfmc-sub">
            Compare, Book, Repair — <strong>Fast. Easy. Affordable.</strong>
          </p>

          <button className="wfmc-btn" onClick={() => window.location.href = "/signup"}>
  Get Help Now
</button>

        </div>
      </section>



      {/* ============================================
          SERVICES SECTION — MODAL VERSION
      ============================================ */}
      <section id="services" className="services">
        <div className="services-inner">

          <h2 data-animate>Our Core Services</h2>
          <p className="services-subtitle" data-animate>
            Fast, reliable and professional on-demand vehicle support across Uganda.
          </p>

          <div className="services-grid">

            {/* GARAGE */}
            <div className="service-card" data-animate>
              <img src="/images/bg2.jpg" alt="Garage" />
              <h3>Garage Support</h3>
              <p>Mechanical repairs anywhere you are.</p>
              <button className="learn-btn" onClick={() => openModal(SERVICES.garage)}>Learn More</button>
            </div>

            {/* FUEL */}
            <div className="service-card" data-animate>
              <img src="/images/fuel.jpg" alt="Fuel Delivery" />
              <h3>Fuel Delivery</h3>
              <p>Fuel delivered whenever you need it.</p>
              <button className="learn-btn" onClick={() => openModal(SERVICES.fuel)}>Learn More</button>
            </div>

            {/* TOWING */}
            <div className="service-card" data-animate>
              <img src="/images/towing.jpg" alt="Towing" />
              <h3>Towing Service</h3>
              <p>Reliable towing support.</p>
              <button className="learn-btn" onClick={() => openModal(SERVICES.towing)}>Learn More</button>
            </div>

            {/* AMBULANCE */}
            <div className="service-card" data-animate>
              <img src="/images/ambulance.jpg" alt="Ambulance" />
              <h3>Ambulance Service</h3>
              <p>Emergency medical response.</p>
              <button className="learn-btn" onClick={() => openModal(SERVICES.ambulance)}>Learn More</button>
            </div>

          </div>
        </div>
      </section>



      {/* ============================================
          WHY CHOOSE US SECTION
      ============================================ */}
      <section id="why" className="why full-width-section">
        <div className="why-inner-wrap">

          <h2 className="section-title" data-animate>Why Choose AutoAID?</h2>
          <p className="section-subtitle" data-animate>
            Trusted by thousands of drivers across Uganda for fast, reliable and certified assistance.
          </p>

          <div className="why-grid">

            <div className="why-card" data-animate>
              <div className="why-inner">
                <div className="why-front glass-card">
                  <div className="icon-circle">📍</div>
                  <h3>Real-Time Tracking</h3>
                </div>
                <div className="why-back glass-card-back">
                  <p>Know exactly where your provider is — live GPS tracking included.</p>
                </div>
              </div>
            </div>

            <div className="why-card" data-animate>
              <div className="why-inner">
                <div className="why-front glass-card">
                  <div className="icon-circle">✔</div>
                  <h3>Verified Experts</h3>
                </div>
                <div className="why-back glass-card-back">
                  <p>Every provider is fully certified and background-checked.</p>
                </div>
              </div>
            </div>

            <div className="why-card" data-animate>
              <div className="why-inner">
                <div className="why-front glass-card">
                  <div className="icon-circle">⚡</div>
                  <h3>Instant Dispatch</h3>
                </div>
                <div className="why-back glass-card-back">
                  <p>We assign the nearest available specialist immediately.</p>
                </div>
              </div>
            </div>

            <div className="why-card" data-animate>
              <div className="why-inner">
                <div className="why-front glass-card">
                  <div className="icon-circle">💳</div>
                  <h3>Secure Payments</h3>
                </div>
                <div className="why-back glass-card-back">
                  <p>Pay safely via Mobile Money or bank — fully encrypted.</p>
                </div>
              </div>
            </div>

          </div>
        </div>
      </section>



      {/* ============================================
          HOW IT WORKS
      ============================================ */}
      <section id="how" className="how">
        <h2 data-animate>How It Works</h2>

        <div className="how-grid">

          <div className="how-step" data-animate>
            <div className="step-circle">01</div>
            <h3>Create an Account</h3>
            <p>Register instantly using your phone number.</p>
          </div>

          <div className="how-step" data-animate>
            <div className="step-circle">02</div>
            <h3>Select a Service</h3>
            <p>Choose from garage, fuel, towing, or ambulance.</p>
          </div>

          <div className="how-step" data-animate>
            <div className="step-circle">03</div>
            <h3>Share Your Location</h3>
            <p>Smart GPS matching connects you to the nearest provider.</p>
          </div>

          <div className="how-step" data-animate>
            <div className="step-circle">04</div>
            <h3>Help Arrives</h3>
            <p>Track your provider in real-time until arrival.</p>
          </div>

        </div>
      </section>




      {/* ============================================
          MODAL
      ============================================ */}
      {modalData && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            
            <button className="modal-close" onClick={closeModal}>✖</button>

            <img src={modalData.image} className="modal-img" alt="service" />

            <h2>{modalData.title}</h2>
            <p>{modalData.desc}</p>

            <button className="modal-action">Request This Service</button>
          </div>
        </div>
      )}

    </div>
  );
}
