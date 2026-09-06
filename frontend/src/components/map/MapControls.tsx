import type L from 'leaflet';
import type { ReactNode } from 'react';

interface MapControlsProps {
  map: L.Map | null;
  onReset: () => void;
  onChooseAirspace: () => void;
  onToggleFilters: () => void;
  filtersActive: boolean;
}

function ControlButton({
  onClick,
  label,
  active,
  children,
}: {
  onClick: () => void;
  label: string;
  active?: boolean;
  children: ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      aria-label={label}
      title={label}
      className={`flex items-center justify-center w-9 h-9 rounded-md border transition-colors ${
        active
          ? 'bg-aero-cyan/20 border-aero-cyan/40 text-aero-cyan'
          : 'bg-aero-surface/95 border-aero-border text-slate-300 hover:bg-aero-muted'
      }`}
    >
      {children}
    </button>
  );
}

export function MapControls({ map, onReset, onChooseAirspace, onToggleFilters, filtersActive }: MapControlsProps) {
  function handleFullscreen() {
    const el = document.querySelector('.leaflet-container') as HTMLElement | null;
    if (!el) return;
    if (!document.fullscreenElement) {
      el.requestFullscreen?.().catch(() => {});
    } else {
      document.exitFullscreen?.().catch(() => {});
    }
  }

  function handleLocateMe() {
    if (!map || !navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        map.flyTo([pos.coords.latitude, pos.coords.longitude], 10, { duration: 0.8 });
      },
      () => {
        // Permission denied/unavailable — fail silently, no error toast (§16).
      },
      { timeout: 5000 }
    );
  }

  return (
    <div className="flex flex-col gap-1.5">
      <ControlButton onClick={onChooseAirspace} label="Choose Airspace">
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </ControlButton>

      <ControlButton onClick={onToggleFilters} label="Traffic filters" active={filtersActive}>
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4h18M6 12h12M10 20h4" />
        </svg>
      </ControlButton>

      <div className="flex flex-col rounded-md border border-aero-border overflow-hidden">
        <button
          onClick={() => map?.zoomIn()}
          aria-label="Zoom in"
          className="flex items-center justify-center w-9 h-9 bg-aero-surface/95 text-slate-300 hover:bg-aero-muted border-b border-aero-border"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
        </button>
        <button
          onClick={() => map?.zoomOut()}
          aria-label="Zoom out"
          className="flex items-center justify-center w-9 h-9 bg-aero-surface/95 text-slate-300 hover:bg-aero-muted"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
          </svg>
        </button>
      </div>

      <ControlButton onClick={onReset} label="Reset / center map">
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
      </ControlButton>

      <ControlButton onClick={handleFullscreen} label="Fullscreen">
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
        </svg>
      </ControlButton>

      <ControlButton onClick={handleLocateMe} label="Locate me">
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8a4 4 0 100 8 4 4 0 000-8z" />
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 2v3m0 14v3M2 12h3m14 0h3" />
        </svg>
      </ControlButton>
    </div>
  );
}
