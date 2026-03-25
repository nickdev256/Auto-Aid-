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
    { label: "Revenue", icon: <FiBarChart2 />, path: "/admin/revenue" },
    { label: "Subscriptions", icon: <FiCreditCard />, path: "/admin/subscriptions" },
    { label: "Reports", icon: <FiFileText />, path: "/admin/reports" },
    { label: "Settings", icon: <FiSettings />, path: "/admin/settings" },
  ];

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/login");
  };

  return (
    <aside className={`admin-sidebar ${open ? "open" : "closed"}`}>
      <div className="sidebar-inner">

        {/* TOP */}
        <div className="sidebar-top">
          <div className="brand-wrap" onClick={() => navigate("/admin")}>
            <div className="brand-logo">A</div>

            {open && (
              <div className="brand-text">
                <h2>AutoAid</h2>
                <span>Admin Panel</span>
              </div>
            )}
          </div>

          <button className="menu-btn" onClick={() => setOpen(!open)}>
            <FiMenu size={20} />
          </button>
        </div>

        {/* MENU */}
        <nav className="sidebar-menu">
          {menu.map((item, i) => {
            const active =
              item.path === "/admin"
                ? location.pathname === "/admin"
                : location.pathname.startsWith(item.path);

            return (
              <button
                key={i}
                className={`sidebar-item ${active ? "active" : ""}`}
                onClick={() => navigate(item.path)}
                title={!open ? item.label : ""}
              >
                <span className="icon">{item.icon}</span>
                {open && <span className="text">{item.label}</span>}
              </button>
            );
          })}
        </nav>

        {/* BOTTOM */}
        <div className="sidebar-bottom">
          <div className="admin-profile-card">
            <div className="profile-avatar">A</div>
            {open && (
              <div className="profile-text">
                <strong>Administrator</strong>
                <span>Control Center</span>
              </div>
            )}
          </div>

          <button className="logout-side-btn" onClick={handleLogout}>
            <span className="icon">
              <FiLogOut />
            </span>
            {open && <span className="text">Logout</span>}
          </button>
        </div>

      </div>
    </aside>
  );
}