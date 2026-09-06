/**
 * Unit conversion utilities — display only.
 * Backend values are always in raw OpenSky units (meters, m/s).
 * These helpers are purely presentational — never re-derive backend-computed values.
 */

/** meters → feet */
export function mToFt(meters: number): number {
  return meters * 3.28084;
}

/** m/s → km/h */
export function msToKmh(ms: number): number {
  return ms * 3.6;
}

/** m/s → knots */
export function msToKnots(ms: number): number {
  return ms * 1.94384;
}

/** Format altitude: meters in, returns "FL360" or "12,000 ft" string */
export function formatAltitude(meters: number | null): string {
  if (meters === null) return 'Unavailable';
  const ft = mToFt(meters);
  const fl = Math.round(ft / 100);
  if (ft >= 10000) return `FL${fl}`;
  return `${Math.round(ft).toLocaleString()} ft`;
}

/** Format speed: m/s in, returns "450 km/h" */
export function formatSpeed(ms: number | null): string {
  if (ms === null) return 'Unavailable';
  return `${Math.round(msToKmh(ms))} km/h`;
}

/** Format vertical rate: m/s in, returns "+12 fpm" */
export function formatVerticalRate(ms: number | null): string {
  if (ms === null) return 'Unavailable';
  const fpm = Math.round(ms * 196.85);
  return fpm >= 0 ? `+${fpm} fpm` : `${fpm} fpm`;
}

/** Format heading: degrees, returns "162°" */
export function formatHeading(deg: number | null): string {
  if (deg === null) return 'Unavailable';
  return `${Math.round(deg)}°`;
}

/** Format coordinates */
export function formatCoords(lat: number | null, lon: number | null): string {
  if (lat === null || lon === null) return 'Unavailable';
  const latStr = `${Math.abs(lat).toFixed(4)}° ${lat >= 0 ? 'N' : 'S'}`;
  const lonStr = `${Math.abs(lon).toFixed(4)}° ${lon >= 0 ? 'E' : 'W'}`;
  return `${latStr}, ${lonStr}`;
}

/** Format distance in km */
export function formatDistance(km: number | null, prefix = ''): string {
  if (km === null) return 'Unavailable';
  return `${prefix}${Math.round(km).toLocaleString()} km`;
}

/**
 * Format a UTC ISO-8601 timestamp in the browser's local timezone.
 * Per contract §AA — all timestamps are UTC ISO-8601 with Z suffix.
 */
export function formatTimestamp(iso: string | null): string {
  if (!iso) return 'Unavailable';
  try {
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(iso));
  } catch {
    return iso;
  }
}

/** Format time only (no date) */
export function formatTime(iso: string | null): string {
  if (!iso) return 'Unavailable';
  try {
    return new Intl.DateTimeFormat(undefined, { timeStyle: 'short' }).format(new Date(iso));
  } catch {
    return iso;
  }
}

/**
 * "X seconds/minutes ago" — computed client-side from backend timestamp every second.
 * This is the ONLY client-side freshness computation allowed (§AH).
 */
export function timeAgo(iso: string | null): string {
  if (!iso) return '';
  try {
    const diffMs = Date.now() - new Date(iso).getTime();
    const s = Math.max(0, Math.floor(diffMs / 1000));
    if (s < 60) return `${s}s ago`;
    const m = Math.floor(s / 60);
    if (m < 60) return `${m}m ago`;
    return `${Math.floor(m / 60)}h ago`;
  } catch {
    return '';
  }
}

/**
 * Compute freshness label from a backend-provided timestamp.
 * Thresholds from contract §Z: ≤20s → LIVE, 20–90s → DELAYED, >90s → UNAVAILABLE.
 * The backend's freshnessLabel from AnalyticsDto is the authoritative source;
 * use this function only when no explicit freshnessLabel is provided.
 */
export function computeFreshnessLabel(
  lastUpdateIso: string | null
): 'LIVE' | 'DELAYED' | 'UNAVAILABLE' {
  if (!lastUpdateIso) return 'UNAVAILABLE';
  try {
    const ageMs = Date.now() - new Date(lastUpdateIso).getTime();
    const ageS = ageMs / 1000;
    if (ageS <= 20) return 'LIVE';
    if (ageS <= 90) return 'DELAYED';
    return 'UNAVAILABLE';
  } catch {
    return 'UNAVAILABLE';
  }
}

/** Convert Unix epoch seconds to ISO string (for TrailResponseDto.points[].timestamp) */
export function epochSecondsToIso(seconds: number): string {
  return new Date(seconds * 1000).toISOString();
}

/** Format delay in minutes */
export function formatDelay(minutes: number | null): string {
  if (minutes === null) return 'Unavailable';
  const clamped = Math.max(0, minutes);
  if (clamped < 1) return '< 1 min';
  return `${Math.round(clamped)} min`;
}

/** Format probability 0–1 as percentage */
export function formatProbability(prob: number | null): string {
  if (prob === null) return 'Unavailable';
  return `${Math.round(prob * 100)}%`;
}
