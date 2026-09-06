import { useState, useEffect } from 'react';
import { NavLink, Link } from 'react-router-dom';
import { Logo } from './Logo';
import { AccountMenu } from './AccountMenu';
import { NotificationBell } from '../alerts/NotificationBell';
import { GlobalSearch } from '../common/GlobalSearch';
import { useAuthStore } from '../../store/authStore';

const NAV_ITEMS = [
  { to: '/map', label: 'Map' },
  { to: '/flights', label: 'Flights' },
  { to: '/airports', label: 'Airports' },
  { to: '/analytics', label: 'Analytics' },
  { to: '/my-flights', label: 'My Flights' },
];

export function Header() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);

  useEffect(() => {
    if (mobileSearchOpen) document.body.style.overflow = 'hidden';
    else document.body.style.overflow = '';
    return () => {
      document.body.style.overflow = '';
    };
  }, [mobileSearchOpen]);

  return (
    <>
      <header className="sticky top-0 z-40 h-14 md:h-16 bg-aero-surface/95 backdrop-blur border-b border-aero-border">
        <div className="h-full max-w-[1600px] mx-auto px-3 md:px-6 flex items-center gap-4">
          <Link to="/" className="shrink-0">
            <Logo size={26} />
          </Link>

          <nav className="hidden md:flex items-center gap-1 shrink-0">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    isActive ? 'text-aero-cyan bg-aero-cyan/10' : 'text-slate-300 hover:text-slate-100 hover:bg-aero-muted'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="hidden md:block flex-1 max-w-md ml-auto">
            <GlobalSearch />
          </div>

          <button
            className="md:hidden ml-auto flex items-center justify-center w-9 h-9 rounded-md hover:bg-aero-muted"
            onClick={() => setMobileSearchOpen(true)}
            aria-label="Search"
          >
            <svg className="w-5 h-5 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>

          <div className="flex items-center gap-2 shrink-0">
            {isAuthenticated ? (
              <>
                <NotificationBell />
                <AccountMenu />
              </>
            ) : (
              <>
                <Link to="/login" className="btn-secondary text-sm px-3 py-1.5 hidden sm:inline-flex">
                  Log in
                </Link>
                <Link to="/register" className="btn-primary text-sm px-3 py-1.5">
                  Sign up
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {mobileSearchOpen && (
        <div className="fixed inset-0 z-50 bg-aero-base/98 backdrop-blur-sm p-4 animate-fade-in md:hidden">
          <div className="flex items-center gap-3 mb-4">
            <div className="flex-1">
              <GlobalSearch onClose={() => setMobileSearchOpen(false)} />
            </div>
            <button
              onClick={() => setMobileSearchOpen(false)}
              className="text-sm text-state-gray px-2 py-1"
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </>
  );
}
