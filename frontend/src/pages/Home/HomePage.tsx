import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { analyticsService } from '../../services/analyticsService';
import { LiveIndicator } from '../../components/common';
import { computeFreshnessLabel } from '../../utils/formatters';
import { AIRSPACE_DEFAULT_CITY_ID } from '../../utils/config';

const FEATURES = [
  {
    title: 'Live Flight Tracking',
    desc: 'Real-time aircraft positions across major world airspaces, updated continuously.',
    to: '/map',
    cta: 'Open the map',
  },
  {
    title: 'Flight Intelligence',
    desc: 'Search any flight by callsign or ICAO24 and see its live position, route, and status.',
    to: '/flights',
    cta: 'Search flights',
  },
  {
    title: 'AI Delay Prediction',
    desc: 'Machine-learning delay estimates for scheduled flights, with an honest probability readout.',
    to: '/flights',
    cta: 'View predictions',
  },
  {
    title: 'Conflict-Risk Detection',
    desc: 'Calculated closest-approach analysis between nearby aircraft — a research/decision-support signal.',
    to: '/map',
    cta: 'See it on the map',
  },
  {
    title: 'Airport Information',
    desc: 'Search airports worldwide and inspect their current conditions and coordinates.',
    to: '/airports',
    cta: 'Browse airports',
  },
  {
    title: 'Analytics',
    desc: 'Live traffic stats, delay distributions, and system health at a glance.',
    to: '/analytics',
    cta: 'View analytics',
  },
];

export function HomePage() {
  const { data } = useQuery({
    queryKey: ['analytics-home'],
    queryFn: () => analyticsService.get('today', AIRSPACE_DEFAULT_CITY_ID),
    refetchInterval: 20_000,
    retry: 1,
  });

  const freshness = data ? computeFreshnessLabel(data.systemHealth.lastOpenSkyUpdate) : 'UNAVAILABLE';

  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden border-b border-aero-border">
        <div className="max-w-6xl mx-auto px-4 md:px-8 py-14 md:py-20 grid md:grid-cols-2 gap-10 items-center">
          <div>
            <p className="text-xs font-semibold tracking-widest text-aero-cyan mb-3">
              AI-BASED AIR TRAFFIC MONITORING &amp; PREDICTION
            </p>
            <h1 className="text-3xl md:text-5xl font-bold text-slate-50 leading-tight mb-4">
              Real air traffic.<br />Real predictions.
            </h1>
            <p className="text-slate-400 text-base md:text-lg mb-8 max-w-md">
              AeroAI shows live aircraft over India and major world airspaces, with
              AI-based delay predictions and calculated conflict-risk signals — all
              grounded in real data, never fabricated.
            </p>
            <div className="flex flex-wrap gap-3">
              <Link to="/map" className="btn-primary px-6 py-3 text-base">
                Explore Live Traffic
              </Link>
              <Link to="/flights" className="btn-secondary px-6 py-3 text-base">
                Search a flight
              </Link>
            </div>
          </div>

          <div className="panel p-5">
            <div className="flex items-center justify-between mb-4">
              <span className="text-xs font-semibold tracking-widest text-state-gray">LIVE SNAPSHOT — MUMBAI</span>
              <LiveIndicator freshnessLabel={freshness} lastUpdate={data?.systemHealth.lastOpenSkyUpdate ?? null} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <StatBlock label="Aircraft visible" value={data?.liveTraffic.totalAircraftVisible} />
              <StatBlock label="In flight" value={data?.liveTraffic.byStatus.IN_FLIGHT} />
              <StatBlock label="Potential conflicts" value={data?.conflictStats.potentialConflicts} />
              <StatBlock label="Tracked flights" value={data?.systemHealth.totalTrackedFlights} />
            </div>
            <Link to="/map" className="block text-center mt-5 text-sm text-aero-cyan hover:underline">
              View the full live map →
            </Link>
          </div>
        </div>
      </section>

      {/* Feature grid */}
      <section className="max-w-6xl mx-auto px-4 md:px-8 py-14">
        <h2 className="text-sm font-semibold tracking-widest text-state-gray mb-6">WHAT AEROAI DOES</h2>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {FEATURES.map((f) => (
            <Link
              key={f.title}
              to={f.to}
              className="panel p-5 hover:border-aero-cyan/40 transition-colors group"
            >
              <h3 className="text-base font-semibold text-slate-100 mb-2 group-hover:text-aero-cyan transition-colors">
                {f.title}
              </h3>
              <p className="text-sm text-slate-400 mb-4">{f.desc}</p>
              <span className="text-sm text-aero-cyan">{f.cta} →</span>
            </Link>
          ))}
        </div>
      </section>

      <footer className="border-t border-aero-border py-6 text-center text-xs text-state-gray">
        AeroAI is a research/decision-support platform. Predictions and conflict-risk
        signals are estimates, not operational guarantees — always defer to official ATC
        and airline sources.
      </footer>
    </div>
  );
}

function StatBlock({ label, value }: { label: string; value: number | undefined | null }) {
  return (
    <div>
      <p className="text-2xl font-semibold font-mono text-slate-100">
        {value !== undefined && value !== null ? value : '—'}
      </p>
      <p className="text-xs text-state-gray mt-0.5">{label}</p>
    </div>
  );
}
