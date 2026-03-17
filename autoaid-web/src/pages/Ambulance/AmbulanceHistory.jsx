// src/pages/Ambulance/AmbulanceHistoryAdvanced.jsx
import React, { useEffect, useMemo, useState, useRef } from "react";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import jsPDF from "jspdf";
import html2canvas from "html2canvas";
import "./AmbulanceHistory.css";

const EMOJI = {
  accident: "🚗💥",
  stroke: "🧠⚡",
  labour: "🤰",
  medical: "🏥",
};

const STATUS_ORDER = ["pending", "assigned", "arrived", "transporting", "completed"];

export default function AmbulanceHistoryAdvanced() {
  const { user } = useAuth();
  const nav = useNavigate();

  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);

  // UI state
  const [q, setQ] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [conditionFilter, setConditionFilter] = useState("all");
  const [sortBy, setSortBy] = useState("newest"); // newest | oldest
  const [selected, setSelected] = useState(null); // selected request for modal
  const [showModal, setShowModal] = useState(false);

  const containerRef = useRef(null);

  useEffect(() => {
    if (!user) return;
    load();
    // eslint-disable-next-line
  }, [user]);

  const load = async () => {
    setLoading(true);
    try {
      const id = user.id || user._id;
      const res = await axios.get(`http://localhost:5001/api/ambulance/history/${id}`);
      // ensure consistent shape (requestId present)
      const items = (res.data || []).map((r) => ({
        ...r,
        requestId: r.requestId || r.id || r._id,
      }));
      setList(items);
    } catch (err) {
      console.error("History load error", err);
    } finally {
      setLoading(false);
    }
  };

  // Filtered & sorted list
  const filtered = useMemo(() => {
    let out = [...list];

    if (q.trim()) {
      const qq = q.toLowerCase();
      out = out.filter((r) =>
        (r.userName || "").toLowerCase().includes(qq) ||
        (r.meta?.emergencyType || "").toLowerCase().includes(qq) ||
        (r.assignedToName || "").toLowerCase().includes(qq)
      );
    }

    if (statusFilter !== "all") out = out.filter((r) => r.status === statusFilter);
    if (conditionFilter !== "all") out = out.filter((r) => r.meta?.condition === conditionFilter);

    out.sort((a, b) => {
      const ta = new Date(a.createdAt).getTime();
      const tb = new Date(b.createdAt).getTime();
      return sortBy === "newest" ? tb - ta : ta - tb;
    });

    return out;
  }, [list, q, statusFilter, conditionFilter, sortBy]);

  // Download PDF for a particular request
  const downloadReport = async (req) => {
    try {
      // create a DOM snapshot (small card) then convert to PDF
      const node = document.createElement("div");
      node.style.padding = "16px";
      node.style.maxWidth = "500px";
      node.style.fontFamily = "Arial, sans-serif";
      node.innerHTML = `
        <h2>Ambulance Request Report</h2>
        <p><strong>Request ID:</strong> ${req.requestId}</p>
        <p><strong>Type:</strong> ${req.meta?.emergencyType || "-"}</p>
        <p><strong>Condition:</strong> ${req.meta?.condition || "-"}</p>
        <p><strong>Requested at:</strong> ${new Date(req.createdAt).toLocaleString()}</p>
        <p><strong>Status:</strong> ${req.status}</p>
        <p><strong>Assigned:</strong> ${req.assignedToName || "—"}</p>
        <p><strong>Address:</strong> ${req.address || `${req.lat}, ${req.lng}`}</p>
        <p><strong>Notes:</strong> ${req.note || req.meta?.note || "-"}</p>
      `;
      document.body.appendChild(node);
      const canvas = await html2canvas(node, { scale: 2 });
      const imgData = canvas.toDataURL("image/png");
      document.body.removeChild(node);

      const pdf = new jsPDF({ unit: "px", format: "a4" });
      const pageWidth = pdf.internal.pageSize.getWidth();
      const imgProps = pdf.getImageProperties(imgData);
      const imgWidth = pageWidth - 40;
      const imgHeight = (imgProps.height * imgWidth) / imgProps.width;
      pdf.addImage(imgData, "PNG", 20, 20, imgWidth, imgHeight);
      pdf.save(`ambulance-request-${req.requestId}.pdf`);
    } catch (err) {
      console.error("PDF error", err);
      alert("Failed to create PDF. Install html2canvas & jspdf.");
    }
  };

  const openRequest = (r) => {
    
    nav(`/ambulance/status/${r.requestId}`);
  };

  const requestAgain = (r) => {
  
    nav("/ambulance/request", { state: { prefill: r } });
  };

  const openModal = (r) => {
    setSelected(r);
    setShowModal(true);
  };

  const closeModal = () => {
    setSelected(null);
    setShowModal(false);
  };


  const osmLink = (lat, lng) => `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=16/${lat}/${lng}`;

  return (
    <div className="ah-root" ref={containerRef}>
      <div className="ah-panel">

        <div className="ah-header">
          <h2>🚑 Ambulance Request History</h2>
          <p className="subtitle">All your past ambulance calls — click a row for details or actions</p>
        </div>

        <div className="ah-controls">
          <input placeholder="Search by type, driver or name..." value={q} onChange={(e) => setQ(e.target.value)} />

          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="all">All Statuses</option>
            <option value="pending">Pending</option>
            <option value="assigned">Assigned</option>
            <option value="arrived">Arrived</option>
            <option value="transporting">Transporting</option>
            <option value="completed">Completed</option>
          </select>

          <select value={conditionFilter} onChange={(e) => setConditionFilter(e.target.value)}>
            <option value="all">All Conditions</option>
            <option value="critical">Critical</option>
            <option value="serious">Serious</option>
            <option value="stable">Stable</option>
          </select>

          <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
            <option value="newest">Newest first</option>
            <option value="oldest">Oldest first</option>
          </select>

          <button className="refresh" onClick={load}>⟳ Refresh</button>
        </div>

        <div className="ah-list">
          {loading ? (
            <div className="empty">Loading…</div>
          ) : filtered.length === 0 ? (
            <div className="empty">No requests found</div>
          ) : (
            filtered.map((r, i) => {
              const type = r.meta?.emergencyType || "medical";
              const emoji = EMOJI[type] || "🚑";
              const condition = r.meta?.condition || "unknown";
              const timestamp = new Date(r.createdAt).toLocaleString();
              const mapUrl = r.lat && r.lng ? osmLink(r.lat, r.lng) : null;
              return (
                <div key={r.requestId} className="ah-item" style={{ animationDelay: `${i * 0.04}s` }}>
                  <div className="item-left">
                    <div className="icon">{emoji}</div>
                    <div className="meta">
                      <div className="title" onClick={() => openRequest(r)}>{(type || "").toUpperCase()}</div>
                      <div className="small">{r.userName} • {timestamp}</div>
                    </div>
                  </div>

                  <div className="item-mid">
                    <div className={`severity ${condition}`}>{condition}</div>
                    <div className={`status-badge status-${r.status.replace(/\s+/g, "")}`}>{r.status}</div>
                    <div className="tiny-map">
                      {mapUrl ? (
                        <a href={mapUrl} target="_blank" rel="noreferrer">Open map</a>
                      ) : (
                        <span className="muted">No GPS</span>
                      )}
                    </div>
                  </div>

                  <div className="item-right">
                    <button onClick={() => openModal(r)} className="btn small">Details</button>
                    <button onClick={() => downloadReport(r)} className="btn small">Download</button>
                    <button onClick={() => requestAgain(r)} className="btn small ghost">Request Again</button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Modal */}
      {showModal && selected && (
        <div className="ah-modal" onClick={closeModal}>
          <div className="ah-modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Request {selected.requestId}</h3>
            <p><strong>Type:</strong> {selected.meta?.emergencyType}</p>
            <p><strong>Condition:</strong> {selected.meta?.condition}</p>
            <p><strong>Requested:</strong> {new Date(selected.createdAt).toLocaleString()}</p>
            <p><strong>Status:</strong> {selected.status}</p>
            <p><strong>Assigned:</strong> {selected.assignedToName || "—"}</p>
            <p><strong>Address:</strong> {selected.address || `${selected.lat}, ${selected.lng}`}</p>
            <p><strong>Notes:</strong> {selected.note || selected.meta?.note || "—"}</p>

      
            <div className="modal-timeline">
              <h4>Timeline</h4>
             
              <div className="tl-row">
                <span className="dot" /> <div><strong>Requested</strong> — {new Date(selected.createdAt).toLocaleString()}</div>
              </div>
              <div className="tl-row">
                <span className="dot" /> <div><strong>Current status</strong> — {selected.status}</div>
              </div>
            </div>

            <div className="modal-actions">
              <button onClick={() => openRequest(selected)} className="btn">Open Status</button>
              <button onClick={() => downloadReport(selected)} className="btn">Download PDF</button>
              <button onClick={() => { requestAgain(selected); closeModal(); }} className="btn ghost">Request Again</button>
              <button onClick={closeModal} className="btn ghost small">Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
