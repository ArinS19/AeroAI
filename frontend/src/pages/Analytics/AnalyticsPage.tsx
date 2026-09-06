import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import { analyticsService } from '../../services/analyticsService';
import { KpiCard } from '../../components/analytics/KpiCard';
import { LoadingSpinner, ErrorState, LiveIndicator, CalcBadge } from '../../components/common';
import { AirspaceSelector } from '../../components/map/AirspaceSelector';
import { formatTimestamp } from '../../utils/formatters';
import { AIRSPACE_DEFAULT_CITY_ID } from '../../utils/config';
import { extractErrorMessage } from '../../api/client';

const PIE_COLORS: Record<string, string> = {
  IN_FLIGHT: '#3B82F6',
  LANDED: '#22C55E',
  SCHEDULED: '#22D3EE',
  UNKNOWN: '#6B7280',
};

const HEALTH_DOT: Record<string, string> = {
  UP: 'bg-state-green',
  DEGRADED: 'bg-state-amber',
  UNKNOWN: 'bg-state-gray',
};

export function AnalyticsPage() {
  const [params, setParams] = useSearchParams();
  const [range, setRange] = useState<'today' | '7d'>('today');
  const [selectorOpen, setSelectorOpen] = useState(false);
  const cityId = params.get('airspace') || AIRSPACE_DEFAULT_CITY_ID;

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['analytics', range, cityId],
    queryFn: () => analyticsService.get(range, cityId),
    refetchInterval: 30_000,
  });

  const altitudeData = data
    ? Object.entries(data.liveTraffic.byAltitudeBand).map(([band, count]) => ({ band, count }))
    : [];

  const statusData = data
    ? Object.entries(data.liveTraffic.byStatus).map(([status, count]) => ({ status, count }))
    : [];

  const delayCategoryData = data
    ? Object.entries(data.flightStats.delayCategoryDistribution).map(([category, count]) => ({ category, count }))
    : [];

  return (
    <div className="max-w-6xl mx-auto px-4 md:px-8 py-6">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
        <div>
          <h1 className="text-xl font-semibold text-slate-100">Analytics</h1>
          <p className="text-sm text-state-gray mt-0.5">{cityId}</p>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex bg-aero-muted rounded-md p-0.5">
            <button
              onClick={() => setRange('today')}
              className={`px-3 py-1.5 text-xs rounded-md transition-colors ${range === 'today' ? 'bg-aero-cyan/20 text-aero-cyan' : 'text-state-gray'}`}
            >
              Today
            </button>
            <button
              onClick={() => setRange('7d')}
              className={`px-3 py-1.5 text-xs rounded-md transition-colors ${range === '7d' ? 'bg-aero-cyan/20 text-aero-cyan' : 'text-state-gray'}`}
            >
              Last 7 Days
            </button>
          </div>
          <button onClick={() => setSelectorOpen(true)} className="btn-secondary text-xs px-3 py-1.5">
            Change airspace
          </button>
        </div>
      </div>

      {isLoading && <LoadingSpinner message="Loading analytics…" className="p-10 justify-center" />}
      {isError && <ErrorState message={extractErrorMessage(error) || 'Could not load analytics.'} onRetry={() => refetch()} />}

      {data && (
        <div className="space-y-6">
          {/* Live traffic */}
          <section>
            <h2 className="text-xs font-semibold tracking-widest text-state-gray mb-3">LIVE TRAFFIC</h2>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-4">
              <KpiCard label="Aircraft visible" value={data.liveTraffic.totalAircraftVisible} />
              <KpiCard label="Arrivals" value={data.liveTraffic.arrivalsInRange} />
              <KpiCard label="Departures" value={data.liveTraffic.departuresInRange} />
              <KpiCard label="Tracked flights (system)" value={data.systemHealth.totalTrackedFlights} />
            </div>
            <div className="grid sm:grid-cols-2 gap-4">
              <div className="panel p-4">
                <p className="text-xs text-state-gray mb-3">Aircraft by altitude band</p>
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={altitudeData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1E2A35" />
                    <XAxis dataKey="band" tick={{ fontSize: 11, fill: '#6B7280' }} />
                    <YAxis tick={{ fontSize: 11, fill: '#6B7280' }} allowDecimals={false} />
                    <Tooltip contentStyle={{ background: '#12171D', border: '1px solid #1E2A35', fontSize: 12 }} />
                    <Bar dataKey="count" fill="#22D3EE" radius={[3, 3, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <div className="panel p-4">
                <p className="text-xs text-state-gray mb-3">Aircraft by status</p>
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie data={statusData} dataKey="count" nameKey="status" cx="50%" cy="50%" outerRadius={80}>
                      {statusData.map((d) => (
                        <Cell key={d.status} fill={PIE_COLORS[d.status] ?? '#6B7280'} />
                      ))}
                    </Pie>
                    <Legend wrapperStyle={{ fontSize: 11 }} />
                    <Tooltip contentStyle={{ background: '#12171D', border: '1px solid #1E2A35', fontSize: 12 }} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>
          </section>

          {/* Flight statistics */}
          <section>
            <h2 className="text-xs font-semibold tracking-widest text-state-gray mb-3">FLIGHT STATISTICS</h2>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-4">
              <KpiCard label="Flights monitored" value={data.flightStats.flightsMonitored} />
              <KpiCard label="Active flights" value={data.flightStats.activeFlights} />
              <KpiCard label="Completed flights" value={data.flightStats.completedFlights} />
              <KpiCard
                label="Avg. AI-predicted delay"
                value={data.flightStats.averagePredictedDelayMinutes !== null ? `${Math.round(data.flightStats.averagePredictedDelayMinutes)} min` : null}
                badge="AI"
              />
            </div>
            {delayCategoryData.length > 0 && (
              <div className="panel p-4">
                <p className="text-xs text-state-gray mb-3">AI-predicted delay category distribution</p>
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart data={delayCategoryData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1E2A35" />
                    <XAxis dataKey="category" tick={{ fontSize: 11, fill: '#6B7280' }} />
                    <YAxis tick={{ fontSize: 11, fill: '#6B7280' }} allowDecimals={false} />
                    <Tooltip contentStyle={{ background: '#12171D', border: '1px solid #1E2A35', fontSize: 12 }} />
                    <Bar dataKey="count" fill="#3B82F6" radius={[3, 3, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </section>

          {/* Conflict statistics */}
          <section>
            <div className="flex items-center gap-2 mb-3">
              <h2 className="text-xs font-semibold tracking-widest text-state-gray">CONFLICT STATISTICS</h2>
              <CalcBadge tooltip="AeroAI-calculated conflict-risk indicators — research/decision-support only." />
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-4">
              <KpiCard label="Potential conflicts" value={data.conflictStats.potentialConflicts} />
              <KpiCard label="Medium risk" value={data.conflictStats.mediumRiskCount} />
              <KpiCard label="High risk" value={data.conflictStats.highRiskCount} />
            </div>
            <div className="panel p-4">
              <p className="text-xs text-state-gray mb-3">Recent conflict-risk events</p>
              {data.conflictStats.recentConflictEvents.length === 0 ? (
                <p className="text-sm text-state-gray italic">No recent conflict-risk events.</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs text-state-gray border-b border-aero-border">
                      <th className="py-1.5 pr-4">AIRCRAFT</th>
                      <th className="py-1.5 pr-4">OTHER AIRCRAFT</th>
                      <th className="py-1.5 pr-4">RISK</th>
                      <th className="py-1.5 pr-4">CALCULATED AT</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.conflictStats.recentConflictEvents.map((e, i) => (
                      <tr key={i} className="border-b border-aero-border/50">
                        <td className="py-1.5 pr-4 font-mono text-slate-300">{e.icao24 ?? 'Unavailable'}</td>
                        <td className="py-1.5 pr-4 font-mono text-slate-300">{e.otherIcao24}</td>
                        <td className="py-1.5 pr-4">
                          <span className={`badge ${e.riskLevel === 'HIGH' ? 'bg-state-red/20 text-state-red border border-state-red/30' : 'bg-state-amber/20 text-state-amber border border-state-amber/30'}`}>
                            {e.riskLevel}
                          </span>
                        </td>
                        <td className="py-1.5 pr-4 text-slate-400">{formatTimestamp(e.calculatedAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </section>

          {/* System health */}
          <section>
            <h2 className="text-xs font-semibold tracking-widest text-state-gray mb-3">SYSTEM HEALTH</h2>
            <div className="panel p-4 grid sm:grid-cols-2 gap-4">
              <div>
                <p className="text-xs text-state-gray mb-1">Live data freshness</p>
                <LiveIndicator freshnessLabel={data.systemHealth.freshnessLabel} lastUpdate={data.systemHealth.lastOpenSkyUpdate} />
              </div>
              <div className="flex flex-wrap gap-4">
                <HealthDot label="OpenSky" status={data.systemHealth.openSkyStatus} />
                <HealthDot label="Open-Meteo" status={data.systemHealth.openMeteoStatus} />
                <HealthDot label="ML service" status={data.systemHealth.mlServiceStatus} />
              </div>
            </div>
          </section>
        </div>
      )}

      <AirspaceSelector
        open={selectorOpen}
        onClose={() => setSelectorOpen(false)}
        currentCityId={cityId}
        onSelect={(id) => setParams({ airspace: id })}
      />
    </div>
  );
}

function HealthDot({ label, status }: { label: string; status: string }) {
  return (
    <div className="flex items-center gap-2">
      <span className={`w-2 h-2 rounded-full ${HEALTH_DOT[status] ?? 'bg-state-gray'}`} />
      <span className="text-sm text-slate-300">{label}</span>
      <span className="text-xs text-state-gray">{status}</span>
    </div>
  );
}
