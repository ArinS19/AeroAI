interface KpiCardProps {
  label: string;
  value: string | number | null;
  badge?: string;
  hint?: string;
}

export function KpiCard({ label, value, badge, hint }: KpiCardProps) {
  return (
    <div className="panel p-4">
      <div className="flex items-center justify-between mb-1">
        <p className="text-xs text-state-gray tracking-wide">{label}</p>
        {badge && <span className="badge bg-aero-blue/10 text-aero-blue border border-aero-blue/30 text-[10px]">{badge}</span>}
      </div>
      <p className="text-2xl font-semibold font-mono text-slate-100">
        {value === null || value === undefined ? 'Unavailable' : value}
      </p>
      {hint && <p className="text-xs text-state-gray mt-1">{hint}</p>}
    </div>
  );
}
