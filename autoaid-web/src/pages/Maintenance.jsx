import React from "react";

export default function Maintenance() {
  let payload = {
    systemName: "AutoAid",
    message: "AutoAid is currently under maintenance. Please try again later.",
  };

  try {
    const raw = sessionStorage.getItem("maintenance_payload");
    if (raw) payload = { ...payload, ...JSON.parse(raw) };
  } catch {
    // ignore
  }

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#f4f7fb",
        fontFamily: "Poppins, sans-serif",
        padding: 24,
      }}
    >
      <div
        style={{
          maxWidth: 560,
          width: "100%",
          background: "#fff",
          borderRadius: 20,
          padding: 32,
          boxShadow: "0 10px 30px rgba(0,0,0,0.08)",
          textAlign: "center",
        }}
      >
        <div style={{ fontSize: 56, marginBottom: 12 }}>🚧</div>
        <h1 style={{ marginBottom: 12 }}>{payload.systemName || "AutoAid"} Maintenance</h1>
        <p style={{ fontSize: 16, color: "#445", lineHeight: 1.6 }}>
          {payload.message || "AutoAid is currently under maintenance. Please try again later."}
        </p>
      </div>
    </div>
  );
}