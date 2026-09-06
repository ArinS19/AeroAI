import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { authService } from '../../services/authService';
import { useAuthStore } from '../../store/authStore';
import { LoadingSpinner, ErrorState } from '../../components/common';
import { formatTimestamp } from '../../utils/formatters';
import { extractErrorMessage } from '../../api/client';

export function AccountPage() {
  const navigate = useNavigate();
  const clearAuth = useAuthStore((s) => s.clearAuth);

  const { data: user, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['me'],
    queryFn: () => authService.getMe(),
  });

  async function handleLogout() {
    try {
      await authService.logout();
    } finally {
      clearAuth();
      navigate('/');
    }
  }

  return (
    <div className="max-w-lg mx-auto px-4 md:px-8 py-6">
      <h1 className="text-xl font-semibold text-slate-100 mb-5">Account</h1>

      {isLoading && <LoadingSpinner message="Loading account…" className="justify-center p-10" />}
      {isError && <ErrorState message={extractErrorMessage(error) || 'Could not load account.'} onRetry={() => refetch()} />}

      {user && (
        <div className="panel p-6">
          <div className="flex items-center gap-4 mb-6">
            <div className="w-12 h-12 rounded-full bg-aero-muted border border-aero-border flex items-center justify-center text-lg font-semibold text-aero-cyan">
              {user.email.charAt(0).toUpperCase()}
            </div>
            <div>
              <p className="text-base font-medium text-slate-100">{user.email}</p>
              <p className="text-xs text-state-gray">Member since {formatTimestamp(user.createdAt)}</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 mb-6 pb-6 border-b border-aero-border">
            <div>
              <p className="text-xs text-state-gray mb-1">Tracked flights</p>
              <p className="text-2xl font-semibold font-mono text-slate-100">{user.trackedFlightCount}</p>
            </div>
          </div>

          <button onClick={handleLogout} className="btn-secondary w-full text-state-red">
            Log out
          </button>
        </div>
      )}
    </div>
  );
}
