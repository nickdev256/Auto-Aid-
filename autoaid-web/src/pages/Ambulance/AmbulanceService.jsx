import React from "react";
import { useNavigate } from "react-router-dom";
import { FaArrowLeft } from "react-icons/fa";
import "./AmbulanceService.css";

export default function AmbulanceService() {
  const nav = useNavigate();

  const cards = [
    {
      title: "Ambulance Service",
      subtitle:
        "Immediate emergency assistance. The nearest available ambulance will be assigned automatically.",
      img: "/images/Ambulace.jpeg",
      buttons: [
        {
          text: "Request Ambulance",
          action: () => nav("/ambulance/request"),
          primary: true,
        },
        {
          text: "View History",
          action: () => nav("/ambulance/history"),
          primary: false,
        },
      ],
    },
    {
      title: "How It Works",
      img: "/images/CAR.jpeg",
      list: [
        "We detect your location automatically",
        "The nearest ambulance is assigned",
        "The driver contacts you immediately",
        "You can track the ambulance in real-time",
      ],
    },
    {
      title: "Important Contacts",
      img: "/images/call.jpeg",
      list: ["National Emergency Line: 999", "Health Center IV Hotline: 0800 100 066"],
    },
    {
      title: "Safety Tips",
      img: "/images/safety.jpg",
      list: [
        "Stay calm and stay with the patient.",
        "Provide clear directions when the driver calls.",
        "Keep your phone nearby and reachable.",
      ],
    },
  ];

  return (
    <div className="ambulance-page">
      
      {/* Back Button */}
      <div className="back-button" onClick={() => nav(-1)}>
        <FaArrowLeft /> Back
      </div>

      {/* CENTERED EMERGENCY BUTTON */}
      <div className="quick-request-btn">
        <button
          onClick={() => nav("/ambulance/request")}
          className="big-btn"
        >
          🚑 Request Ambulance Now
        </button>
      </div>

      {/* CARDS */}
      {cards.map((card, index) => (
        <div key={index} className="ambulance-card glass-card balanced-card">
          <div className="card-content">
            <div className="card-illustration">
              <img src={card.img} alt={card.title} />
            </div>

            <div className="card-text">
              <h2>{card.title}</h2>

              {card.subtitle && <p className="subtitle">{card.subtitle}</p>}

              {card.list && (
                <ul>
                  {card.list.map((item, i) => (
                    <li key={i}>✔️ {item}</li>
                  ))}
                </ul>
              )}

              {card.buttons && (
                <div className="btn-group">
                  {card.buttons.map((btn, i) => (
                    <button
                      key={i}
                      className={btn.primary ? "btn-primary" : "btn-secondary"}
                      onClick={btn.action}
                    >
                      {btn.text}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      ))}

      {/* Info Banner */}
      <div className="status-banner">
        🚨 <strong>Average response time: 3–7 minutes</strong>
      </div>
    </div>
  );
}
