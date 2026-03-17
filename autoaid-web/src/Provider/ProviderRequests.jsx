import React, { useEffect, useState } from "react";
import { getRequestsByProvider, assignRequest } from "../../services/api";

export default function ProviderRequests({ providerId }) {
  const [requests, setRequests] = useState([]);

  const fetchRequests = async () => {
    try {
      const data = await getRequestsByProvider(providerId);
      setRequests(data);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    fetchRequests();
    const interval = setInterval(fetchRequests, 5000); // refresh every 5s for near real-time
    return () => clearInterval(interval);
  }, [providerId]);

  const handleAssign = async (requestId) => {
    try {
      const assigned = await assignRequest(requestId, providerId);
      setRequests(prev => prev.map(r => r.id === assigned.id ? assigned : r));
    } catch (err) {
      alert(err.message);
    }
  };

  return (
    <div>
      <h2>Active Requests</h2>
      {requests.length === 0 ? <p>No active requests</p> :
        requests.map(r => (
          <div key={r.id} style={{ border: "1px solid #ccc", margin: 8, padding: 8 }}>
            <p><strong>{r.userName}</strong> ({r.serviceType})</p>
            <p>Location: {r.address}</p>
            <p>Status: {r.status}</p>
            {r.status === "pending" && <button onClick={() => handleAssign(r.id)}>Accept</button>}
          </div>
        ))
      }
    </div>
  );
}
