import { useState, useEffect } from 'react';
import type { FreshnessLabel } from '../../types';
import { timeAgo } from '../../utils/formatters';

interface LiveIndicatorProps {
  freshnessLabel: FreshnessLabel;
  lastUpdate: string | null;
  className?: string;
}

/**
 * Ticking freshness indicator per §Z / §17.
 * Shows LIVE / DATA DELAYED / LIVE DATA UNAVAILABLE.
 * "Xs ago" text updates every second client-side from backend timestamp.
 */
export function LiveIndicator({ freshnessLabel, lastUpdate, className = '' }: LiveIndicatorProps) {
  const [, forceUpdate] = useState(0);

  // Tick every second to keep "Xs ago" fresh
  useEffect(() => {
    const interval = setInterval(() => forceUpdate((n) => n + 1), 1000);
    return () => clearInterval(interval);
  }, []);

  if (freshnessLabel === 'LIVE') {
    return (
      <div className={`flex items-center gap-2 ${className}`}>
        <span className="relative flex h-2 w-2">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-state-green opacity-75" />
          <span className="relative inline-flex rounded-full h-2 w-2 bg-state-green" />
        </span>
        <span className="text-xs font-semibold tracking-widest text-state-green">LIVE</span>
        {lastUpdate && (
          <span className="text-xs text-slate-500">· Updated {timeAgo(lastUpdate)}</span>
        )}
      </div>
    );
  }

  if (freshnessLabel === 'DELAYED') {
    return (
      <div className={`flex items-center gap-2 ${className}`}>
        <span className="inline-flex h-2 w-2 rounded-full bg-state-amber" />
        <span className="text-xs font-semibold tracking-widest text-state-amber">DATA DELAYED</span>
        {lastUpdate && (
          <span className="text-xs text-slate-500">· Last update {timeAgo(lastUpdate)}</span>
        )}
      </div>
    );
  }

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <span className="inline-flex h-2 w-2 rounded-full bg-state-gray" />
      <span className="text-xs font-semibold tracking-widest text-state-gray">LIVE DATA UNAVAILABLE</span>
    </div>
  );
}
