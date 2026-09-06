import { Routes, Route } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { ProtectedRoute } from './app/ProtectedRoute';
import { HomePage } from './pages/Home/HomePage';
import { MapPage } from './pages/Map/MapPage';
import { FlightsPage } from './pages/Flights/FlightsPage';
import { FlightDetailsPage } from './pages/FlightDetails/FlightDetailsPage';
import { AirportsPage } from './pages/Airports/AirportsPage';
import { AirportDetailsPage } from './pages/AirportDetails/AirportDetailsPage';
import { AnalyticsPage } from './pages/Analytics/AnalyticsPage';
import { MyFlightsPage } from './pages/MyFlights/MyFlightsPage';
import { AccountPage } from './pages/Account/AccountPage';
import { LoginPage } from './pages/Login/LoginPage';
import { RegisterPage } from './pages/Register/RegisterPage';
import { NotFoundPage } from './pages/NotFound/NotFoundPage';

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/map" element={<MapPage />} />
        <Route path="/flights" element={<FlightsPage />} />
        <Route path="/flights/:icao24" element={<FlightDetailsPage />} />
        <Route path="/airports" element={<AirportsPage />} />
        <Route path="/airports/:id" element={<AirportDetailsPage />} />
        <Route path="/analytics" element={<AnalyticsPage />} />
        <Route
          path="/my-flights"
          element={
            <ProtectedRoute>
              <MyFlightsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/account"
          element={
            <ProtectedRoute>
              <AccountPage />
            </ProtectedRoute>
          }
        />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
