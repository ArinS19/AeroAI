import L from 'leaflet';

export type MarkerColor = 'default' | 'selected' | 'medium' | 'high';

const COLORS: Record<MarkerColor, string> = {
  default: '#22D3EE',
  selected: '#F8FAFC',
  medium: '#F59E0B',
  high: '#EF4444',
};

/**
 * Builds a Leaflet DivIcon for an aircraft marker: a rotatable plane
 * silhouette (rotated to match true heading, 0deg = north) plus an
 * optional callsign label per the zoom-band rules in §13.4.
 */
export function buildAircraftIcon(opts: {
  heading: number | null;
  color: MarkerColor;
  label: string | null;
  showLabel: boolean;
  onGround: boolean | null;
}): L.DivIcon {
  const { heading, color, label, showLabel, onGround } = opts;
  const rotation = heading ?? 0;
  const fill = COLORS[color];
  const size = color === 'selected' ? 26 : 20;
  const opacity = onGround ? 0.55 : 1;

  const labelHtml =
    showLabel && label
      ? `<div class="aero-marker-label" style="position:absolute;top:${size + 2}px;left:50%;transform:translateX(-50%);white-space:nowrap;font-size:10px;line-height:1;font-family:'JetBrains Mono',monospace;color:#E2E8F0;background:rgba(10,14,18,0.75);padding:1px 4px;border-radius:3px;border:1px solid rgba(30,42,53,0.9);pointer-events:none;">${label}</div>`
      : '';

  const glow = color === 'selected' ? `filter:drop-shadow(0 0 4px ${fill});` : '';

  const html = `
    <div style="position:relative;width:${size}px;height:${size}px;opacity:${opacity};">
      <div style="width:100%;height:100%;transform:rotate(${rotation}deg);transform-origin:center center;${glow}">
        <svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="${fill}">
          <path d="M21 16v-2l-8-5V3.5c0-.83-.67-1.5-1.5-1.5S10 2.67 10 3.5V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z"/>
        </svg>
      </div>
      ${labelHtml}
    </div>
  `;

  return L.divIcon({
    html,
    className: 'aero-aircraft-icon',
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  });
}

/** Small distinct marker for nearby airports — never confusable with aircraft. */
export function buildAirportIcon(): L.DivIcon {
  const html = `
    <div style="width:16px;height:16px;display:flex;align-items:center;justify-content:center;">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#22C55E" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-2 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
      </svg>
    </div>
  `;
  return L.divIcon({
    html,
    className: 'aero-airport-icon',
    iconSize: [16, 16],
    iconAnchor: [8, 8],
  });
}
