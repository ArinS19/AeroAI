/**
 * Frontend-wide tunable constants mirroring IMPLEMENT_v2.md §94
 * (Configuration Reference) — only the subset relevant to the frontend.
 */

export const MAP_TILE_URL =
  import.meta.env.VITE_MAP_TILE_URL ?? 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';

/** §13.2 default viewport — India + surrounding region. */
export const MAP_DEFAULT_CENTER: [number, number] = [22.0, 79.0];
export const MAP_DEFAULT_ZOOM = 5;

/** §94 — AIRSPACE_DEFAULT_CITY_ID */
export const AIRSPACE_DEFAULT_CITY_ID = 'mumbai';

/** §17 / §94 */
export const LIVE_THRESHOLD_SECONDS = 20;
export const UNAVAILABLE_THRESHOLD_SECONDS = 90;

/** §13.4 — callsign label zoom bands. */
export const ZOOM_LABELS_SELECTED_ONLY = 7; // 7-9: selected/conflict aircraft only
export const ZOOM_LABELS_ALL = 10; // >=10: all aircraft

export const AERO_TOKEN_KEY = 'aeroai_token';
export const AERO_USER_KEY = 'aeroai_user';
