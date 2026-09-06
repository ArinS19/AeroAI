import { Outlet, useLocation } from 'react-router-dom';
import { Header } from './Header';
import { BottomNav } from './BottomNav';

/**
 * Global app shell: header + routed page content + mobile bottom nav.
 * The Map page manages its own full-bleed layout below the header
 * (§13.1) — everything else gets standard page padding/scrolling.
 */
export function AppLayout() {
  const location = useLocation();
  const isMapPage = location.pathname.startsWith('/map');

  return (
    <div className="min-h-screen flex flex-col bg-aero-base">
      <Header />
      {/*
        The Map page needs a fully DEFINITE height all the way down to the
        Leaflet container (Leaflet's panes are 100% absolutely-positioned,
        so they contribute zero natural/intrinsic height to their
        ancestors). Relying on `flex-1` alone inside a `min-h-screen`
        (min-height, not height) column is an indefinite-size edge case
        that some layouts resolve to a real height and others collapse to
        zero — which is exactly why the map rendered a blank area while
        every other page (whose content has real in-flow height regardless
        of the flex computation) looked fine. Using an explicit
        `calc(100vh - <header height>)` here removes the ambiguity
        entirely for the one page that actually needs a pixel-exact,
        guaranteed-non-zero viewport.
      */}
      <main className={isMapPage ? 'h-[calc(100vh-56px)] md:h-[calc(100vh-64px)]' : 'flex-1 pb-16 md:pb-0'}>
        <Outlet />
      </main>
      <BottomNav />
    </div>
  );
}
