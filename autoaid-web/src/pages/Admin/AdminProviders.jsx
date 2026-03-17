import React, { useEffect, useState } from "react";
import { getPendingProviders, approveProvider, activateSubscription } from "../../services/api";
import "./ProviderManagement.css";

export default function ProviderManagement() {
  const [pendingProviders, setPendingProviders] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadPending = async () => {
    setLoading(true);
    try {
      const data = await getPendingProviders();
      setPendingProviders(data);
    } catch (err) {
      console.error("Error fetching pending providers:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPending();
  }, []);

  const handleApprove = async (id) => {
    try {
      await approveProvider(id);
      await loadPending();
    } catch (err) {
      alert("Error: " + err.message);
    }
  };

  const handleSubscribe = async (id) => {
    try {
      await activateSubscription(id);
      await loadPending();
    } catch (err) {
      alert("Error: " + err.message);
    }
  };

  if (loading) return <p>Loading pending providers...</p>;

  return (
    <div className="provider-management-container">
      <h2>Pending Provider Requests</h2>

      {pendingProviders.length === 0 ? (
        <p>No pending providers</p>
      ) : (
        <table className="provider-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Service Type</th>
              <th>Subscription</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {pendingProviders.map(p => (
              <tr key={p.id}>
                <td>{p.name}</td>
                <td>{p.email}</td>
                <td>{p.businessType}</td>
                <td>{p.subscription}</td>
                <td>{p.status}</td>
                <td className="action-buttons">
                  <button onClick={() => handleApprove(p.id)} className="btn-approve">
                    Approve
                  </button>
                  <button onClick={() => handleSubscribe(p.id)} className="btn-subscribe">
                    Activate Subscription
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
