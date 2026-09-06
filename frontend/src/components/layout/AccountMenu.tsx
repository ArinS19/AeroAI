import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { authService } from '../../services/authService';

export function AccountMenu() {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const clearAuth = useAuthStore((s) => s.clearAuth);

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  if (!user) return null;
  const initial = user.email.charAt(0).toUpperCase();

  async function handleLogout() {
    setOpen(false);
    try {
      await authService.logout();
    } finally {
      clearAuth();
      navigate('/');
    }
  }

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((o) => !o)}
        className="flex items-center justify-center w-9 h-9 rounded-full bg-aero-muted border border-aero-border text-sm font-semibold text-aero-cyan hover:bg-aero-border transition-colors"
        aria-label="Account menu"
      >
        {initial}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-56 bg-aero-surface border border-aero-border rounded-lg shadow-2xl z-50 animate-fade-in overflow-hidden">
          <div className="px-4 py-3 border-b border-aero-border">
            <p className="text-sm font-medium text-slate-200 truncate">{user.email}</p>
            <p className="text-xs text-state-gray">{user.trackedFlightCount} tracked flight{user.trackedFlightCount === 1 ? '' : 's'}</p>
          </div>
          <button
            onClick={() => { setOpen(false); navigate('/my-flights'); }}
            className="w-full text-left px-4 py-2.5 text-sm text-slate-200 hover:bg-aero-muted transition-colors"
          >
            My Flights
          </button>
          <button
            onClick={() => { setOpen(false); navigate('/account'); }}
            className="w-full text-left px-4 py-2.5 text-sm text-slate-200 hover:bg-aero-muted transition-colors"
          >
            Account
          </button>
          <button
            onClick={handleLogout}
            className="w-full text-left px-4 py-2.5 text-sm text-state-red hover:bg-aero-muted transition-colors border-t border-aero-border"
          >
            Log out
          </button>
        </div>
      )}
    </div>
  );
}
