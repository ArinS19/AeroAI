// ============================================================
// AeroAI — TypeScript DTO types
// These mirror the exact backend DTOs from BACKEND_FRONTEND_HANDOFF.md.
// DO NOT add fields not in the contract; DO NOT derive/compute values here.
// ============================================================

// ─── Auth ────────────────────────────────────────────────────────────────────

export interface UserDto {
  id: number;
  email: string;
  createdAt: string; // ISO-8601 UTC
  trackedFlightCount: number;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserDto;
}

export interface RegisterRequest {
  email: string;
  password: string;
  confirmPassword: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// ─── Error envelope ───────────────────────────────────────────────────────────

export interface ApiError {
  timestamp: string;
  status: number;
  error: string; // VALIDATION_ERROR | UNAUTHORIZED | FORBIDDEN | NOT_FOUND | CONFLICT | INTERNAL_ERROR | UPSTREAM_UNAVAILABLE
  message: string;
  path: string;
}

// ─── Airspaces ────────────────────────────────────────────────────────────────

export interface AirportRefDto {
  id: number;
  name: string;
  iata: string | null;
  icao: string | null;
  latitude: number;
  longitude: number;
}

export interface AirspaceDto {
  cityId: string; // slug e.g. "mumbai"
  cityName: string;
  country: string;
  latitude: number;
  longitude: number;
  defaultZoom: number;
  lamin: number;
  lomin: number;
  lamax: number;
  lomax: number;
  displayRegion: string | null;
  aliases: string[];
  nearbyAirports: AirportRefDto[]; // only populated on detail endpoint
}

// ─── Airports ─────────────────────────────────────────────────────────────────

export interface AirportDto {
  id: number;
  name: string;
  iata: string | null;
  icao: string | null;
  city: string | null;
  location: string | null;
  country: string | null;
  latitude: number;
  longitude: number;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // 0-indexed current page
  size: number;
}

// ─── Aircraft / Flights ───────────────────────────────────────────────────────

/**
 * Flight status enum values — EXACTLY as returned by the backend.
 * LOW conflict risk is implicit/absent; it never appears as a literal string in conflicts[].
 */
export type FlightStatus = 'SCHEDULED' | 'IN_FLIGHT' | 'LANDED' | 'UNKNOWN';
export type ConflictRiskLevel = 'MEDIUM' | 'HIGH'; // LOW is implicit/absent
export type PredictionCategory = 'Low' | 'Moderate' | 'High'; // mixed-case from ML service
export type AlertType = 'STATUS_CHANGE' | 'DELAY_CATEGORY_CHANGE' | 'CONFLICT_RISK_CHANGE' | 'DATA_UNAVAILABLE';
export type FreshnessLabel = 'LIVE' | 'DELAYED' | 'UNAVAILABLE';
export type SystemHealthStatus = 'UP' | 'DEGRADED' | 'UNKNOWN';

/**
 * AircraftDto — used by /api/flights/current AND WebSocket added/updated entries.
 * Units: altitude=meters, speed=m/s, heading=degrees clockwise from north.
 * Any field may be null — render as "Unavailable", NEVER as 0.
 */
export interface AircraftDto {
  icao24: string;
  callsign: string | null;
  latitude: number | null;
  longitude: number | null;
  altitude: number | null; // meters
  speed: number | null;    // m/s
  heading: number | null;  // degrees clockwise from true north
  verticalRate: number | null; // m/s, positive=climbing
  onGround: boolean | null;
  status: FlightStatus;
  lastUpdate: string; // ISO-8601 UTC
}

/**
 * FlightListItemDto — Flights page table rows / search results.
 */
export interface FlightListItemDto {
  icao24: string;
  callsign: string | null;
  airline: string | null;
  origin: AirportRefDto | null;      // currently always null
  destination: AirportRefDto | null; // currently always null
  status: FlightStatus;
  altitude: number | null; // meters
  speed: number | null;    // m/s
  lastUpdate: string;
}

export interface WeatherDto {
  temperatureCelsius: number | null;
  conditionDescription: string | null;
  windSpeedKmh: number | null;
  windDirectionDegrees: number | null;
  humidityPercent: number | null;
  visibilityKm: number | null;
  pressureHpa: number | null;
  cloudCoverPercent: number | null;
  precipitationMm: number | null;
}

export interface PredictionDto {
  available: boolean;
  predictedDelayMinutes: number | null;
  delayProbability: number | null;
  delayOver15Minutes: boolean | null;
  category: PredictionCategory | null;
  disclaimer: string; // "Machine-learning estimate, not an airline guarantee."
}

export interface ConflictRiskDto {
  otherIcao24: string;
  otherCallsign: string | null;
  riskLevel: ConflictRiskLevel;
  horizontalDistanceKm: number;
  altitudeDifferenceM: number;
  closureRateMs: number; // negative=closing, positive=opening
  dcpaHorizontalM: number;
  altitudeDiffAtTcpaM: number;
  timeToClosestApproachSeconds: number;
  calculatedAt: string;
  disclaimer: string; // "Research/decision-support estimate only..."
}

export interface CurrentPositionDto {
  latitude: number | null;
  longitude: number | null;
  altitude: number | null; // meters
  speed: number | null;    // m/s
  heading: number | null;
  verticalRate: number | null;
  lastUpdate: string;
}

/**
 * FlightDetailDto — full details for /api/flights/{icao24}.
 */
export interface FlightDetailDto {
  icao24: string;
  callsign: string | null;
  airline: string | null;
  aircraftRegistration: string | null;
  aircraftType: string | null;
  origin: AirportRefDto | null;      // currently always null
  destination: AirportRefDto | null; // currently always null
  departureTime: string | null;
  arrivalTime: string | null;
  status: FlightStatus;
  currentPosition: CurrentPositionDto | null;
  distanceTravelledKm: number | null;
  distanceRemainingKm: number | null; // currently always null
  estimatedArrival: string | null;    // currently always null
  trailAvailable: boolean;
  weather: WeatherDto | null;
  prediction: PredictionDto;
  conflicts: ConflictRiskDto[];
  tracked: boolean;
}

// ─── Trail ────────────────────────────────────────────────────────────────────

export interface TrailPoint {
  lat: number;
  lon: number;
  timestamp: number; // Unix epoch SECONDS (not ISO-8601)
}

export interface TrailResponseDto {
  icao24: string;
  minutes: number;
  available: boolean;
  points: TrailPoint[];
}

// ─── WebSocket delta ──────────────────────────────────────────────────────────

/**
 * AircraftDeltaMessage — received on /topic/aircraft/{cityId}.
 * added/updated contain full AircraftDto records (not partial diffs).
 * removed is a flat array of icao24 strings.
 */
export interface AircraftDeltaMessage {
  timestamp: string;
  added: AircraftDto[];
  updated: AircraftDto[];
  removed: string[];
}

// ─── Tracking ─────────────────────────────────────────────────────────────────

export interface TrackedFlightDto {
  id: number;
  icao24: string;
  callsign: string | null;
  status: FlightStatus;
  predictedDelayMinutes: number | null;
  delayCategory: PredictionCategory | null;
  lastUpdate: string;
  dataAvailable: boolean;
}

// ─── Alerts ───────────────────────────────────────────────────────────────────

export interface AlertDto {
  id: number;
  trackedFlightId: number;
  type: AlertType;
  message: string;
  createdAt: string;
  read: boolean;
}

export interface UnreadCountDto {
  unreadCount: number;
}

// ─── Analytics ────────────────────────────────────────────────────────────────

export interface LiveTrafficDto {
  totalAircraftVisible: number;
  byAltitudeBand: Record<string, number>;
  byStatus: Partial<Record<FlightStatus, number>>;
  arrivalsInRange: number;
  departuresInRange: number;
}

export interface FlightStatsDto {
  flightsMonitored: number;
  activeFlights: number;
  completedFlights: number;
  averagePredictedDelayMinutes: number | null;
  delayCategoryDistribution: Record<string, number>;
}

export interface ConflictStatsDto {
  potentialConflicts: number;
  mediumRiskCount: number;
  highRiskCount: number;
  recentConflictEvents: Array<{
    icao24: string | null;
    otherIcao24: string;
    riskLevel: ConflictRiskLevel;
    calculatedAt: string;
  }>;
}

export interface SystemHealthDto {
  lastOpenSkyUpdate: string | null;
  freshnessLabel: FreshnessLabel;
  openSkyStatus: SystemHealthStatus;
  openMeteoStatus: SystemHealthStatus;
  mlServiceStatus: SystemHealthStatus;
  totalTrackedFlights: number;
}

export interface AnalyticsDto {
  range: 'today' | '7d';
  airspaceId: string;
  liveTraffic: LiveTrafficDto;
  flightStats: FlightStatsDto;
  conflictStats: ConflictStatsDto;
  systemHealth: SystemHealthDto;
}

// ─── UI-only helpers ──────────────────────────────────────────────────────────

/** Valid trail duration values accepted by the trail endpoint. */
export type TrailMinutes = 5 | 15 | 30 | 60;
