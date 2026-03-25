import React from "react";
import { Routes, Route, useLocation } from "react-router-dom";

import Navbar from "./components/Navbar.jsx";
import Footer from "./components/Footer.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";

// AUTH & LANDING
import LandingPage from "./pages/Landing/LandingPage.jsx";
import Login from "./pages/Auth/Login.jsx";
import Signup from "./pages/Auth/Signup.jsx";
import OTPVerify from "./pages/Auth/OTPVerify.jsx";

// DASHBOARD
import Dashboard from "./pages/Dashboard/Dashboard.jsx";

// TOWING
import TowingService from "./pages/Towing/TowingService.jsx";
import TowingRequestForm from "./pages/Towing/TowingRequestForm.jsx";
import TowingHistory from "./pages/Towing/TowingHistory.jsx";
import TowingStatus from "./pages/Towing/TowingStatus.jsx";
import TowingMap from "./pages/Towing/TowingMap.jsx";
import TowingActive from "./pages/Towing/TowingActive.jsx";
import TowingTrack from "./pages/Towing/TowingTrack.jsx";

// AMBULANCE
import AmbulanceService from "./pages/Ambulance/AmbulanceService.jsx";
import AmbulanceRequestForm from "./pages/Ambulance/AmbulanceRequestForm.jsx";
import AmbulanceHistory from "./pages/Ambulance/AmbulanceHistory.jsx";
import AmbulanceStatus from "./pages/Ambulance/AmbulanceStatus.jsx";
import AmbulanceMap from "./pages/Ambulance/AmbulanceMap.jsx";

// FUEL
import FuelService from "./pages/Fuel/FuelService.jsx";
import FuelRequestForm from "./pages/Fuel/FuelRequestForm.jsx";
import FuelHistory from "./pages/Fuel/FuelHistory.jsx";
import FuelStatus from "./pages/Fuel/FuelStatus.jsx";
import FuelActive from "./pages/Fuel/FuelActive.jsx";

// GARAGE
import GarageService from "./pages/Garage/GarageService.jsx";
import GarageRequest from "./pages/Garage/GarageRequest.jsx";
import ActiveRequest from "./pages/Garage/ActiveRequest.jsx";
import TrackRequest from "./pages/Garage/TrackRequest.jsx";
import NearbyGarages from "./pages/Garage/NearbyGarages.jsx";

// CHAT
import UniversalUserChat from "./pages/Chat/UniversalUserChat.jsx";
import UniversalProviderChatWrapper from "./pages/Chat/UniversalProviderChatWrapper.jsx";

// PROVIDER
import ProviderSignup from "./Provider/ProviderSignup.jsx";
import ProviderSubscription from "./Provider/ProviderSubscription.jsx";
import ProviderHome from "./Provider/ProviderHome.jsx";
import ProviderGarageDashboard from "./Provider/ProviderGarageDashboard.jsx";
import ProviderFuelDashboard from "./Provider/ProviderFuelDashboard.jsx";
import ProviderTowingDashboard from "./Provider/ProviderTowingDashboard.jsx";
import ProviderAmbulanceDashboard from "./Provider/ProviderAmbulanceDashboard.jsx";
import ProviderRequestDetails from "./Provider/ProviderRequestDetails.jsx";
import ProviderMap from "./Provider/ProviderMap.jsx";
import ProviderPending from "./Provider/ProviderPending.jsx";
import ProviderRejected from "./Provider/ProviderRejected.jsx";
import BusinessSettings from "./Provider/BusinessSettings.jsx";

// ADMIN
import AdminDashboard from "./pages/Admin/AdminDashboard.jsx";
import ProviderManagement from "./pages/Admin/ProviderManagement.jsx";
import Reports from "./pages/Admin/Reports.jsx";
import AdminSubscriptions from "./pages/Admin/AdminSubscriptions.jsx";
import Settings from "./pages/Admin/Settings.jsx";
import Requests from "./pages/Admin/Requests.jsx";
import AdminChatList from "./pages/Admin/AdminChatList.jsx";
import AdminChat from "./pages/Admin/AdminChat.jsx";
import AdminUsers from "./pages/Admin/AdminUsers.jsx";
import AdminRevenueDashboard from "./pages/Admin/AdminRevenueDashboard.jsx";

// MAINTENANCE
import Maintenance from "./pages/Maintenance";

import "./App.css";

export default function App() {
  const location = useLocation();

  const isAdminRoute = location.pathname.startsWith("/admin");
  const isProviderRoute = location.pathname.startsWith("/provider");
  const isAuthRoute =
    location.pathname.startsWith("/login") ||
    location.pathname.startsWith("/signup") ||
    location.pathname.startsWith("/otp");

  const isUserAppRoute =
    location.pathname.startsWith("/dashboard") ||
    location.pathname.startsWith("/fuel") ||
    location.pathname.startsWith("/garage") ||
    location.pathname.startsWith("/towing") ||
    location.pathname.startsWith("/ambulance") ||
    location.pathname.startsWith("/chat");

  const hideUI = isAuthRoute || isAdminRoute || isProviderRoute || isUserAppRoute;
  const isFullWidth = isAdminRoute;

  return (
    <div className="app-root">
      {!hideUI && <Navbar />}

      <main
        className={`main-content ${
          isFullWidth ? "full-layout" : "standard-layout"
        }`}
      >
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/otp" element={<OTPVerify />} />
          <Route path="/maintenance" element={<Maintenance />} />

          <Route
            path="/dashboard"
            element={
              <ProtectedRoute role="user">
                <Dashboard />
              </ProtectedRoute>
            }
          />

          <Route path="/towing" element={<TowingService />} />
          <Route path="/towing/request" element={<TowingRequestForm />} />
          <Route path="/towing/history" element={<TowingHistory />} />
          <Route path="/towing/status/:id" element={<TowingStatus />} />
          <Route path="/towing/map/:id" element={<TowingMap />} />
          <Route path="/towing/active" element={<TowingActive />} />
          <Route path="/towing/track/:id" element={<TowingTrack />} />

          <Route path="/ambulance" element={<AmbulanceService />} />
          <Route path="/ambulance/request" element={<AmbulanceRequestForm />} />
          <Route path="/ambulance/history" element={<AmbulanceHistory />} />
          <Route path="/ambulance/status/:id" element={<AmbulanceStatus />} />
          <Route path="/ambulance/map/:id" element={<AmbulanceMap />} />

          <Route path="/fuel" element={<FuelService />} />
          <Route path="/fuel/request" element={<FuelRequestForm />} />
          <Route path="/fuel/history" element={<FuelHistory />} />
          <Route path="/fuel/status/:id" element={<FuelStatus />} />
          <Route path="/fuel/active" element={<FuelActive />} />

          <Route path="/garage" element={<GarageService />} />
          <Route path="/garage/request" element={<GarageRequest />} />
          <Route path="/garage/active" element={<ActiveRequest />} />
          <Route path="/garage/track/:id" element={<TrackRequest />} />
          <Route path="/nearby-garages" element={<NearbyGarages />} />

          <Route
            path="/garage/chat/:requestId"
            element={<UniversalUserChat />}
          />

          <Route path="/provider/signup" element={<ProviderSignup />} />
          <Route path="/provider/subscription" element={<ProviderSubscription />} />
          <Route path="/provider/settings" element={<BusinessSettings />} />
          <Route path="/provider/pending" element={<ProviderPending />} />
          <Route path="/provider/rejected" element={<ProviderRejected />} />
          <Route path="/provider/details/:id" element={<ProviderRequestDetails />} />
          <Route path="/provider/map/:id" element={<ProviderMap />} />
          <Route
            path="/provider/chat/:requestId"
            element={<UniversalProviderChatWrapper />}
          />

          <Route
            path="/provider/dashboard"
            element={
              <ProtectedRoute role="provider">
                <ProviderHome />
              </ProtectedRoute>
            }
          />
          <Route
            path="/provider/garage"
            element={
              <ProtectedRoute role="provider">
                <ProviderGarageDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/provider/fuel"
            element={
              <ProtectedRoute role="provider">
                <ProviderFuelDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/provider/towing"
            element={
              <ProtectedRoute role="provider">
                <ProviderTowingDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/provider/ambulance"
            element={
              <ProtectedRoute role="provider">
                <ProviderAmbulanceDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin"
            element={
              <ProtectedRoute role="admin">
                <AdminDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/providers"
            element={
              <ProtectedRoute role="admin">
                <ProviderManagement />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/requests"
            element={
              <ProtectedRoute role="admin">
                <Requests />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute role="admin">
                <AdminUsers />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/reports"
            element={
              <ProtectedRoute role="admin">
                <Reports />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/subscriptions"
            element={
              <ProtectedRoute role="admin">
                <AdminSubscriptions />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/settings"
            element={
              <ProtectedRoute role="admin">
                <Settings />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/chats"
            element={
              <ProtectedRoute role="admin">
                <AdminChatList />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/chat/:requestId"
            element={
              <ProtectedRoute role="admin">
                <AdminChat />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/revenue"
            element={
              <ProtectedRoute role="admin">
                <AdminRevenueDashboard />
              </ProtectedRoute>
            }
          />

          <Route path="*" element={<div>404 - Page Not Found</div>} />
        </Routes>
      </main>

      {!hideUI && <Footer />}
    </div>
  );
}