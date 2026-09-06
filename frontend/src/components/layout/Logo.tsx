interface LogoProps {
  size?: number;
  showWordmark?: boolean;
  className?: string;
}

/**
 * AeroAI mark — radar arc + aircraft silhouette, cyan-on-dark.
 * Kept as a single inline SVG (no external asset) so it always renders
 * theme-correct without a network round-trip.
 */
export function Logo({ size = 28, showWordmark = true, className = '' }: LogoProps) {
  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <svg width={size} height={size} viewBox="0 0 32 32" fill="none" aria-hidden="true">
        <circle cx="16" cy="16" r="14.5" stroke="#22D3EE" strokeOpacity="0.35" strokeWidth="1" />
        <circle cx="16" cy="16" r="10" stroke="#22D3EE" strokeOpacity="0.5" strokeWidth="1" />
        <path
          d="M16 16 L16 3 A13 13 0 0 1 27.25 22.5 Z"
          fill="#22D3EE"
          fillOpacity="0.12"
        />
        <path
          d="M16 5.5 L17.1 13.4 L24.6 17.3 L24.6 18.7 L17.1 16.4 L17.6 21.9 L19.6 23.3 L19.6 24.4 L16 23.4 L12.4 24.4 L12.4 23.3 L14.4 21.9 L14.9 16.4 L7.4 18.7 L7.4 17.3 L14.9 13.4 L16 5.5 Z"
          fill="#22D3EE"
        />
        <circle cx="16" cy="16" r="1.4" fill="#0A0E12" />
      </svg>
      {showWordmark && (
        <span className="font-semibold tracking-wide text-slate-100 leading-none">
          Aero<span className="text-aero-cyan">AI</span>
        </span>
      )}
    </div>
  );
}
