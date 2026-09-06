import { useState, useEffect, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { flightsService } from '../../services/flightsService';
import type { FlightDetailDto, TrailMinutes } from '../../types';
import { LoadingSpinner, ErrorState, UnavailableText, StatusBadge, AiBadge, CalcBadge, RiskBadge } from '../common';
import {
  formatAltitude,
  formatSpeed,
  formatHeading,
  formatCoords,
  formatDistance,
  formatTimestamp,
  formatDelay,
  formatProbability,
  timeAgo,
} from '../../utils/formatters';
import { TrackButton } from './TrackButton';
import { TrailMiniMap } from './TrailMiniMap';
import { extractErrorMessage } from '../../api/client';

const TRAIL_OPTIONS: TrailMinutes[] = [5, 15, 30, 60];

interface FlightDetailPanelProps {
  icao24: string;
  onClose?: () => void;
  showMiniMap?: boolean;
  onTrailChange?: (points: [number, number][]) => void;
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="border-b border-aero-border py-4">
      <h3 className="text-xs font-semibold tracking-widest text-state-gray mb-3">{title}</h3>
      {children}
    </div>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 py-1">
      <span className="text-sm text-slate-400">{label}</span>
      <span className="text-sm text-slate-200 avia-num font-mono">{children}</span>
    </div>
  );
}

export function FlightDetailPanel({ icao24, onClose, showMiniMap = true, onTrailChange }: FlightDetailPanelProps) {
  const [trailMinutes, setTrailMinutes] = useState<TrailMinutes>(15);
  const [why, setWhy] = useState(false);

  const { data: flight, isLoading, isError, error, refetch } = useQuery<FlightDetailDto>({
    queryKey: ['flight', icao24],
    queryFn: () => flightsService.getDetail(icao24),
    refetchInterval: 15_000,
  });

  const { data: trail } = useQuery({
    queryKey: ['trail', icao24, trailMinutes],
    queryFn: () => flightsService.getTrail(icao24, trailMinutes),
    enabled: !!flight?.trailAvailable,
  });

  const trailPoints: [number, number][] = (trail?.available ? trail.points : []).map((p) => [p.lat, p.lon]);

  useEffect(() => {
    onTrailChange?.(trailPoints);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trail]);

  if (isLoading) {
    return <LoadingSpinner message="Loading flight…" className="p-6 justify-center" />;
  }

  if (isError || !flight) {
    return (
      <ErrorState
        message={extractErrorMessage(error) || 'Could not load this flight.'}
        onRetry={() => refetch()}
        className="p-6"
      />
    );
  }

  // Route fields (origin/destination/times) are always null today because
  // OpenSky state vectors carry no flight-plan data (§18.2, §BACKEND_FRONTEND_HANDOFF.md).
  // The section always renders so the user sees honest "Unavailable" values
  // rather than a hidden section, per §28/§3.1.
  const hasCurrent = !!flight.currentPosition;
  const hasProgress = flight.distanceTravelledKm !== null || flight.distanceRemainingKm !== null || flight.estimatedArrival !== null;
  const hasWeather = !!flight.weather && Object.values(flight.weather).some((v) => v !== null);

  return (
    <div className="flex flex-col h-full">
      <div className="flex items-start justify-between p-4 border-b border-aero-border">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-semibold text-slate-100">{flight.callsign ?? flight.icao24}</h2>
            <StatusBadge status={flight.status} />
          </div>
          <p className="text-sm text-state-gray mt-0.5">
            {flight.airline ?? 'Unavailable'} · {flight.icao24}
          </p>
        </div>
        {onClose && (
          <button onClick={onClose} className="text-state-gray hover:text-slate-200 text-lg leading-none px-1">
            ✕
          </button>
        )}
      </div>

      <div className="flex-1 overflow-y-auto px-4">
        <Section title="ROUTE">
          {/* Origin and destination are currently unavailable: OpenSky live state
              vectors carry no flight-plan information (route, origin, destination).
              These fields will populate automatically if a reliable schedule source
              is ever added to the backend, without any frontend change. */}
          <Row label="Origin">
            {flight.origin ? (
              `${flight.origin.name}${flight.origin.iata ? ` (${flight.origin.iata})` : ''}`
            ) : (
              <UnavailableText />
            )}
          </Row>
          <Row label="Destination">
            {flight.destination ? (
              `${flight.destination.name}${flight.destination.iata ? ` (${flight.destination.iata})` : ''}`
            ) : (
              <UnavailableText />
            )}
          </Row>
          <Row label="Departure">
            {flight.departureTime ? formatTimestamp(flight.departureTime) : <UnavailableText />}
          </Row>
          <Row label="Arrival">
            {flight.arrivalTime ? formatTimestamp(flight.arrivalTime) : <UnavailableText />}
          </Row>
        </Section>

        {hasCurrent && (
          <Section title="CURRENT">
            <Row label="Position">{formatCoords(flight.currentPosition!.latitude, flight.currentPosition!.longitude)}</Row>
            <Row label="Altitude">{formatAltitude(flight.currentPosition!.altitude)}</Row>
            <Row label="Speed">{formatSpeed(flight.currentPosition!.speed)}</Row>
            <Row label="Heading">{formatHeading(flight.currentPosition!.heading)}</Row>
            <Row label="Last updated">{timeAgo(flight.currentPosition!.lastUpdate) || <UnavailableText />}</Row>
          </Section>
        )}

        {(flight.aircraftRegistration || flight.aircraftType) && (
          <Section title="AIRCRAFT">
            <Row label="Registration">{flight.aircraftRegistration ?? <UnavailableText />}</Row>
            <Row label="Type">{flight.aircraftType ?? <UnavailableText />}</Row>
          </Section>
        )}

        {hasProgress && (
          <Section title="PROGRESS">
            <Row label="Distance travelled">
              {flight.distanceTravelledKm !== null ? (
                <span className="inline-flex items-center gap-1.5">
                  {formatDistance(flight.distanceTravelledKm)} <CalcBadge />
                </span>
              ) : (
                <UnavailableText />
              )}
            </Row>
            <Row label="Distance remaining">
              {flight.distanceRemainingKm !== null ? (
                <span className="inline-flex items-center gap-1.5">
                  {formatDistance(flight.distanceRemainingKm, '~')} <CalcBadge />
                </span>
              ) : (
                <UnavailableText />
              )}
            </Row>
            <Row label="Estimated arrival">
              {flight.estimatedArrival ? (
                <span className="inline-flex items-center gap-1.5">
                  {formatTimestamp(flight.estimatedArrival)} <CalcBadge tooltip="AeroAI-calculated ETA from remaining distance / effective speed" />
                </span>
              ) : (
                <span className="text-state-gray italic text-sm">ETA unavailable</span>
              )}
            </Row>
          </Section>
        )}

        <Section title="WEATHER">
          {hasWeather ? (
            <>
              <Row label="Temperature">{flight.weather!.temperatureCelsius !== null ? `${flight.weather!.temperatureCelsius}°C` : <UnavailableText />}</Row>
              <Row label="Condition">{flight.weather!.conditionDescription ?? <UnavailableText />}</Row>
              <Row label="Wind">{flight.weather!.windSpeedKmh !== null ? `${Math.round(flight.weather!.windSpeedKmh)} km/h` : <UnavailableText />}</Row>
              <Row label="Humidity">{flight.weather!.humidityPercent !== null ? `${flight.weather!.humidityPercent}%` : <UnavailableText />}</Row>
              <Row label="Visibility">{flight.weather!.visibilityKm !== null ? `${flight.weather!.visibilityKm} km` : <UnavailableText />}</Row>
              <Row label="Pressure">{flight.weather!.pressureHpa !== null ? `${flight.weather!.pressureHpa} hPa` : <UnavailableText />}</Row>
              <Row label="Cloud cover">{flight.weather!.cloudCoverPercent !== null ? `${flight.weather!.cloudCoverPercent}%` : <UnavailableText />}</Row>
              <Row label="Precipitation">{flight.weather!.precipitationMm !== null ? `${flight.weather!.precipitationMm} mm` : <UnavailableText />}</Row>
            </>
          ) : (
            <p className="text-sm text-state-gray italic">Weather: Unavailable</p>
          )}
        </Section>

        <Section title="AI DELAY PREDICTION">
          {flight.prediction.available ? (
            <>
              <div className="flex items-center gap-3 mb-2">
                <span className="text-2xl font-semibold font-mono text-slate-100">
                  {formatDelay(flight.prediction.predictedDelayMinutes)}
                </span>
                {flight.prediction.category && <CategoryBadgeInline category={flight.prediction.category} />}
                <AiBadge disclaimer={flight.prediction.disclaimer} />
              </div>
              <p className="text-sm text-slate-400 mb-2">
                {formatProbability(flight.prediction.delayProbability)} probability of &gt;15 min delay
              </p>
              <button
                onClick={() => setWhy((w) => !w)}
                className="text-xs text-aero-cyan hover:underline"
              >
                {why ? 'Hide' : 'Why this prediction?'}
              </button>
              {why && (
                <div className="mt-2 text-xs text-slate-400 bg-aero-muted rounded-md p-3 leading-relaxed">
                  This estimate is produced by a machine-learning model using feature groups
                  including scheduled departure/arrival time, route, airline characteristics,
                  distance, available airport/airline features, and weather-related features.
                  It is a decision-support estimate, not an airline guarantee, and does not
                  attribute the prediction to any single feature.
                </div>
              )}
            </>
          ) : (
            <p className="text-sm text-state-gray italic">AI Delay Prediction: Unavailable</p>
          )}
        </Section>

        <Section title="CONFLICT RISK">
          {flight.conflicts.length === 0 ? (
            <div className="flex items-center gap-2">
              <RiskBadge level="LOW" />
              <span className="text-sm text-state-gray">No elevated conflict risk detected</span>
            </div>
          ) : (
            <div className="space-y-3">
              {flight.conflicts.map((c) => (
                <div key={c.otherIcao24} className="bg-aero-muted rounded-md p-3">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium text-slate-200">
                      {c.otherCallsign ?? c.otherIcao24}
                    </span>
                    <RiskBadge level={c.riskLevel} />
                  </div>
                  <div className="grid grid-cols-2 gap-1 text-xs text-slate-400">
                    <span>Horizontal distance: <span className="text-slate-200">{c.horizontalDistanceKm.toFixed(1)} km</span></span>
                    <span>Altitude diff: <span className="text-slate-200">{Math.round(c.altitudeDifferenceM)} m</span></span>
                    <span>Closure rate: <span className="text-slate-200">{Math.round(c.closureRateMs)} m/s</span></span>
                    <span>TCPA: <span className="text-slate-200">{Math.round(c.timeToClosestApproachSeconds)} s</span></span>
                  </div>
                  <div className="mt-2">
                    <CalcBadge tooltip={c.disclaimer} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </Section>

        {flight.trailAvailable && (
          <Section title="FLIGHT TRAIL">
            <div className="flex gap-1.5 mb-3">
              {TRAIL_OPTIONS.map((m) => (
                <button
                  key={m}
                  onClick={() => setTrailMinutes(m)}
                  className={`text-xs px-2 py-1 rounded border transition-colors ${
                    trailMinutes === m
                      ? 'bg-aero-cyan/20 border-aero-cyan/40 text-aero-cyan'
                      : 'bg-aero-muted border-aero-border text-state-gray'
                  }`}
                >
                  {m}m
                </button>
              ))}
            </div>
            {showMiniMap && (
              trailPoints.length > 1 ? (
                <TrailMiniMap points={trailPoints} />
              ) : (
                <p className="text-sm text-state-gray italic">Trail unavailable / collecting data</p>
              )
            )}
          </Section>
        )}

        <div className="py-4">
          <TrackButton icao24={flight.icao24} callsign={flight.callsign} tracked={flight.tracked} />
        </div>
      </div>
    </div>
  );
}

function CategoryBadgeInline({ category }: { category: NonNullable<FlightDetailDto['prediction']['category']> }) {
  const cls =
    category === 'Low'
      ? 'bg-state-green/20 text-state-green border-state-green/30'
      : category === 'Moderate'
      ? 'bg-state-amber/20 text-state-amber border-state-amber/30'
      : 'bg-state-red/20 text-state-red border-state-red/30';
  return <span className={`badge border ${cls}`}>{category} Delay Risk</span>;
}
