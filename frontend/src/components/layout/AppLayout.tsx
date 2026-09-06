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
      <main className={isMapPage ? 'flex-1 min-h-0' : 'flex-1 pb-16 md:pb-0'}>
        <Outlet />
      </main>
      <BottomNav />
    </div>
  );
}
