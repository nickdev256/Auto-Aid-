// src/pages/Garage/ProviderProfile.jsx
import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { getPublicProviderProfile } from "../../services/api";
import "./ProviderProfile.css";

export default function ProviderProfile() {
  const { id } = useParams();
  const [provider, setProvider] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const data = await getPublicProviderProfile(id);
        setProvider(data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  if (loading) return <p>Loading provider...</p>;
  if (!provider) return <p>Provider not found</p>;

  return (
    <div className="public-provider-profile">
      {provider.logoUrl && (
        <img src={provider.logoUrl} alt="logo" className="provider-logo" />
      )}
      <h1>{provider.businessName || provider.name}</h1>
      <p><strong>Service type:</strong> {provider.businessType}</p>
      <p><strong>Address:</strong> {provider.address}</p>
      <p><strong>Phone:</strong> {provider.phone}</p>

      {provider.servicesOffered?.length > 0 && (
        <>
          <h3>Services Offered</h3>
          <ul>
            {provider.servicesOffered.map((s) => (
              <li key={s}>{s}</li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
