import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import "./ProviderRequestDetails.css";

export default function ProviderRequestDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [request, setRequest] = useState(null);

  const loadRequest = async () => {
    try {
      const res = await axios.get(
        `http://localhost:5001/api/garageRequests/${id}`
      );
      setRequest(res.data);
    } catch (err) {
      console.error("Error loading request", err);
    }
  };

  useEffect(() => {
    loadRequest();
  }, [id]);

  if (!request) return <p>Loading request...</p>;

  return (
    <div className="provider-details-container">
      <h1>Request Details</h1>

      <div className="details-card">
        <p><strong>User:</strong> {request.userName}</p>
        <p><strong>Phone:</strong> {request.userPhone}</p>
        <p><strong>Status:</strong> {request.status}</p>
        <p><strong>Issue:</strong> {request.vehicleInfo}</p>
        <p><strong>Location:</strong> {request.address}</p>

        <div className="actions">
          <button onClick={() => navigate(`/provider/map/${id}`)}>
            View on Map
          </button>

          <button onClick={() => navigate(`/provider/chat/${id}`)}>
            Chat With User
          </button>
        </div>

        <button onClick={() => navigate("/provider/dashboard")} className="back-btn">
          Back
        </button>
      </div>
    </div>
  );
}
