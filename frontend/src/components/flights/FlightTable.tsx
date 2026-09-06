import { useNavigate } from 'react-router-dom';
import type { FlightListItemDto } from '../../types';
import { StatusBadge, UnavailableText } from '../common';
import { formatAltitude, formatSpeed, timeAgo } from '../../utils/formatters';

interface FlightTableProps {
  flights: FlightListItemDto[];
}

export function FlightTable({ flights }: FlightTableProps) {
  const navigate = useNavigate();

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm min-w-[720px]">
        <thead>
          <tr className="text-left text-xs text-state-gray tracking-widest border-b border-aero-border">
            <th className="py-2 pr-4 font-medium">CALLSIGN</th>
            <th className="py-2 pr-4 font-medium">AIRLINE</th>
            <th className="py-2 pr-4 font-medium">FROM</th>
            <th className="py-2 pr-4 font-medium">TO</th>
            <th className="py-2 pr-4 font-medium">STATUS</th>
            <th className="py-2 pr-4 font-medium text-right">ALTITUDE</th>
            <th className="py-2 pr-4 font-medium text-right">SPEED</th>
            <th className="py-2 pr-4 font-medium">LAST UPDATED</th>
          </tr>
        </thead>
        <tbody>
          {flights.map((f) => (
            <tr
              key={f.icao24}
              onClick={() => navigate(`/flights/${f.icao24}`)}
              className="border-b border-aero-border/50 hover:bg-aero-muted/60 cursor-pointer transition-colors"
            >
              <td className="py-2.5 pr-4 font-mono text-slate-200">{f.callsign ?? f.icao24}</td>
              <td className="py-2.5 pr-4 text-slate-300">{f.airline ?? <UnavailableText />}</td>
              <td className="py-2.5 pr-4 text-slate-300">{f.origin?.iata ?? <UnavailableText />}</td>
              <td className="py-2.5 pr-4 text-slate-300">{f.destination?.iata ?? <UnavailableText />}</td>
              <td className="py-2.5 pr-4"><StatusBadge status={f.status} /></td>
              <td className="py-2.5 pr-4 avia-num font-mono">{formatAltitude(f.altitude)}</td>
              <td className="py-2.5 pr-4 avia-num font-mono">{formatSpeed(f.speed)}</td>
              <td className="py-2.5 pr-4 text-slate-400">{timeAgo(f.lastUpdate) || <UnavailableText />}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
