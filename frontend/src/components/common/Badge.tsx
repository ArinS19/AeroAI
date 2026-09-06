import type { FlightStatus, ConflictRiskLevel, PredictionCategory } from '../../types';

interface StatusBadgeProps {
  status: FlightStatus;
}

const STATUS_CONFIG: Record<FlightStatus, { label: string; className: string }> = {
  IN_FLIGHT: { label: 'IN FLIGHT', className: 'bg-aero-blue/20 text-aero-blue border border-aero-blue/30' },
  LANDED:    { label: 'LANDED',    className: 'bg-state-green/20 text-state-green border border-state-green/30' },
  SCHEDULED: { label: 'SCHEDULED', className: 'bg-aero-cyan/20 text-aero-cyan border border-aero-cyan/30' },
  UNKNOWN:   { label: 'UNKNOWN',   className: 'bg-state-gray/20 text-state-gray border border-state-gray/30' },
};

export function StatusBadge({ status }: StatusBadgeProps) {
  const cfg = STATUS_CONFIG[status] ?? STATUS_CONFIG.UNKNOWN;
  return (
    <span className={`badge text-xs tracking-widest ${cfg.className}`}>
      {cfg.label}
    </span>
  );
}

// ─── Conflict risk badge ──────────────────────────────────────────────────────

interface RiskBadgeProps {
  level: ConflictRiskLevel | 'LOW';
}

const RISK_CONFIG: Record<string, { label: string; className: string }> = {
  LOW:    { label: 'LOW',    className: 'bg-state-green/20 text-state-green border border-state-green/30' },
  MEDIUM: { label: 'MEDIUM', className: 'bg-state-amber/20 text-state-amber border border-state-amber/30' },
  HIGH:   { label: 'HIGH',   className: 'bg-state-red/20 text-state-red border border-state-red/30' },
};

export function RiskBadge({ level }: RiskBadgeProps) {
  const cfg = RISK_CONFIG[level] ?? RISK_CONFIG.LOW;
  return (
    <span className={`badge text-xs tracking-widest ${cfg.className}`}>
      {cfg.label} RISK
    </span>
  );
}

// ─── Prediction category badge ────────────────────────────────────────────────

interface CategoryBadgeProps {
  category: PredictionCategory;
}

const CATEGORY_CONFIG: Record<PredictionCategory, { className: string }> = {
  Low:      { className: 'bg-state-green/20 text-state-green border border-state-green/30' },
  Moderate: { className: 'bg-state-amber/20 text-state-amber border border-state-amber/30' },
  High:     { className: 'bg-state-red/20 text-state-red border border-state-red/30' },
};

export function CategoryBadge({ category }: CategoryBadgeProps) {
  const cfg = CATEGORY_CONFIG[category];
  return (
    <span className={`badge text-xs ${cfg.className}`}>
      {category} Delay Risk
    </span>
  );
}

// ─── AI badge (per §AI — must appear on all ML-generated values) ──────────────

export function AiBadge({ disclaimer }: { disclaimer: string }) {
  return (
    <span
      title={disclaimer}
      className="badge bg-aero-cyan/10 text-aero-cyan border border-aero-cyan/30 cursor-help"
    >
      AI
    </span>
  );
}

// ─── Calculated badge (per §3.2) ──────────────────────────────────────────────

export function CalcBadge({ tooltip = 'Calculated by AeroAI from live data' }: { tooltip?: string }) {
  return (
    <span
      title={tooltip}
      className="badge bg-aero-blue/10 text-aero-blue border border-aero-blue/30 cursor-help"
    >
      Est.
    </span>
  );
}
