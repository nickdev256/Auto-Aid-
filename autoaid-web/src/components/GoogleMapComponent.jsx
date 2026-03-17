import React, { useEffect, useState } from "react";
import { GoogleMap, Marker, Polyline, DirectionsRenderer, useJsApiLoader } from "@react-google-maps/api";

export default function GoogleMapComponent({ userPos, mechanicPos }) {
  const [directions, setDirections] = useState(null);

  const { isLoaded } = useJsApiLoader({
    googleMapsApiKey: "AIzaSyDpDDl5y9m29pVgb2TGR2zewyWHRICU1v8",
    libraries: ["places"]
  });

  useEffect(() => {
    if (!isLoaded || !userPos || !mechanicPos) return;

    const directionsService = new google.maps.DirectionsService();

    directionsService.route(
      {
        origin: mechanicPos,
        destination: userPos,
        travelMode: google.maps.TravelMode.DRIVING,
      },
      (result, status) => {
        if (status === google.maps.DirectionsStatus.OK) {
          setDirections(result);
        } else {
          console.error("Directions request failed:", status);
        }
      }
    );
  }, [isLoaded, userPos, mechanicPos]);

  if (!isLoaded) return <p>Loading map...</p>;

  return (
    <GoogleMap
      center={userPos}
      zoom={14}
      mapContainerStyle={{ height: "450px", width: "100%", borderRadius: "12px" }}
    >
      {/* User marker */}
      <Marker position={userPos} label="You" />

      {/* Mechanic marker */}
      {mechanicPos && <Marker position={mechanicPos} label="M" />}

      {/* Route */}
      {directions && <DirectionsRenderer directions={directions} />}
    </GoogleMap>
  );
}
