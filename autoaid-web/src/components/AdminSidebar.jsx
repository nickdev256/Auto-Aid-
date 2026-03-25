import React from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  FiMenu,
  FiHome,
  FiUsers,
  FiClipboard,
  FiBarChart2,
  FiSettings,
  FiCreditCard,
  FiFileText,
  FiShield,
  FiLogOut,
  FiX,
  FiMessageSquare,
} from "react-icons/fi";
import "./AdminSidebar.css";

export default function AdminSidebar({ open, setOpen }) {
  const navigate = useNavigate();
  const location = useLocation();

  const menu = [
    { label: "Dashboard", icon: <FiHome />, path: "/admin" },
    { label: "Users", icon: <FiUsers />, path: "/admin/users" },
    { label: "Providers", icon: <FiShield />, path: "/admin/providers" },
    { label: "Requests", icon: <FiClipboard />, path: "/admin/requests" },
    { label: "Chats", icon: <FiMessageSquare />, path: "/admin/chats" },
    { label: "Revenue", icon: <FiBarChart2 />, path: "/admin/revenue" },
    { label: "Subscriptions", icon: <FiCreditCard />, path: "/admin/subscriptions" },
    { label: "Reports", icon: <FiFileText />, path: "/admin/reports" },
    { label: "Settings", icon: <FiSettings />, path: "/admin/settings" },
  ];

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/login");
  };

  const isActive = (path) => {
    if (path === "/admin") return location.pathname === "/admin";

    if (path === "/admin/chats") {
      return (
        location.pathname === "/admin/chats" ||
        location.pathname.startsWith("/admin/chat/")
      );
    }

    return location.pathname.startsWith(path);
  };

  return (
    <>
      <div
        className={`admin-sidebar-overlay ${open ? "show" : ""}`}
        onClick={() => setOpen(false)}
      />

      <aside className={`admin-sidebar ${open ? "open" : "closed"}`}>
        <div className="sidebar-top">
          <div
            className="sidebar-brand"
            onClick={() => navigate("/admin")}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") navigate("/admin");
            }}
          >
            <div className="sidebar-brand-logo">A</div>

            <div className={`sidebar-brand-text ${open ? "visible" : ""}`}>
              <h2>AutoAid</h2>
              <span>Admin Portal</span>
            </div>
          </div>

          <button
            className="sidebar-toggle-btn desktop-toggle"
            onClick={() => setOpen(!open)}
            type="button"
            aria-label="Toggle sidebar"
          >
            <FiMenu />
          </button>

          <button
            className="sidebar-toggle-btn mobile-close"
            onClick={() => setOpen(false)}
            type="button"
            aria-label="Close sidebar"
          >
            <FiX />
          </button>
        </div>

        <div className="sidebar-nav">
          {menu.map((item) => (
            <button
              key={item.path}
              className={`sidebar-nav-item ${isActive(item.path) ? "active" : ""}`}
              onClick={() => navigate(item.path)}
              type="button"
              title={!open ? item.label : ""}
            >
              <span className="sidebar-nav-icon">{item.icon}</span>
              <span className={`sidebar-nav-text ${open ? "visible" : ""}`}>
                {item.label}
              </span>
            </button>
          ))}
        </div>

        <div className="sidebar-footer">
          <div className="sidebar-user-card">
            <div className="sidebar-user-avatar">A</div>
            <div className={`sidebar-user-meta ${open ? "visible" : ""}`}>
              <strong>Administrator</strong>
              <span>Control Center</span>
            </div>
          </div>

          <button className="sidebar-logout" onClick={handleLogout} type="button">
            <span className="sidebar-nav-icon">
              <FiLogOut />
            </span>
            <span className={`sidebar-nav-text ${open ? "visible" : ""}`}>
              Logout
            </span>
          </button>
        </div>
      </aside>
    </>
  );
}