import express from "express";

const router = express.Router();

router.post("/route", async (req, res) => {
  try {
    const { originLat, originLng, destLat, destLng } = req.body;

    const response = await fetch(
      "https://routes.googleapis.com/directions/v2:computeRoutes",
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Goog-Api-Key": process.env.GOOGLE_MAPS_API_KEY,
          "X-Goog-FieldMask":
            "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline"
        },
        body: JSON.stringify({
          origin: {
            location: {
              latLng: {
                latitude: originLat,
                longitude: originLng
              }
            }
          },
          destination: {
            location: {
              latLng: {
                latitude: destLat,
                longitude: destLng
              }
            }
          },
          travelMode: "DRIVE"
        })
      }
    );

    const data = await response.json();

    const route = data.routes?.[0];

    res.json({
      distanceMeters: route?.distanceMeters || 0,
      duration: route?.duration || "0s",
      encodedPolyline: route?.polyline?.encodedPolyline || ""
    });

  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

export default router;