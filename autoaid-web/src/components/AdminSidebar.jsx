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
} from "react-icons/fi";
import "./AdminSidebar.css";

export default function AdminSidebar({ open, setOpen }) {
  const navigate = useNavigate();
  const location = useLocation();

  const isAdminRoute = location.pathname.startsWith("/admin");
  if (!isAdminRoute) return null;

  const menu = [
    { label: "Dashboard", icon: <FiHome />, path: "/admin" },
    { label: "Users", icon: <FiUsers />, path: "/admin/users" },
    { label: "Providers", icon: <FiUsers />, path: "/admin/providers" },
    { label: "Requests", icon: <FiClipboard />, path: "/admin/requests" },
   { label: "Revenue", icon: <FiBarChart2 />, path: "/admin/revenue" },
    { label: "Reports", icon: <FiBarChart2 />, path: "/admin/reports" },
    { label: "Settings", icon: <FiSettings />, path: "/admin/settings" },
  ];

  return (
    <div className={`admin-sidebar ${open ? "open" : "closed"}`}>
      <div className="sidebar-top">
        <button className="menu-btn" onClick={() => setOpen(!open)}>
          <FiMenu size={20} />
        </button>
        {open && <h2 className="title">Admin</h2>}
      </div>

      <div className="sidebar-menu">
        {menu.map((item, i) => {
          const active = location.pathname === item.path;

          return (
            <div
              key={i}
              className={`sidebar-item ${active ? "active" : ""}`}
              onClick={() => navigate(item.path)}
            >
              <span className="icon">{item.icon}</span>
              {open && <span className="text">{item.label}</span>}
            </div>
          );
        })}
      </div>
    </div>
  );
}