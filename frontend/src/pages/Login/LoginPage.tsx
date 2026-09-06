import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authService } from '../../services/authService';
import { useAuthStore } from '../../store/authStore';
import { extractErrorMessage } from '../../api/client';
import { Logo } from '../../components/layout/Logo';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const setAuth = useAuthStore((s) => s.setAuth);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await authService.login({ email, password });
      setAuth(res.token, res.user);
      const returnTo = params.get('returnTo');
      navigate(returnTo ? decodeURIComponent(returnTo) : '/');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-full flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <div className="flex justify-center mb-8">
          <Link to="/">
            <Logo size={36} />
          </Link>
        </div>

        <div className="panel p-6">
          <h1 className="text-lg font-semibold text-slate-100 mb-1">Log in</h1>
          <p className="text-sm text-state-gray mb-6">Track flights and manage alerts.</p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1.5">Email</label>
              <input
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="aero-input"
                placeholder="you@example.com"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1.5">Password</label>
              <input
                type="password"
                required
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="aero-input"
                placeholder="••••••••"
              />
            </div>

            {error && (
              <p className="text-sm text-state-red bg-state-red/10 border border-state-red/30 rounded-md px-3 py-2">
                {error}
              </p>
            )}

            <button type="submit" disabled={loading} className="btn-primary w-full">
              {loading ? 'Logging in…' : 'Log in'}
            </button>
          </form>

          <p className="text-sm text-state-gray mt-6 text-center">
            Don't have an account?{' '}
            <Link to="/register" className="text-aero-cyan hover:underline">
              Sign up
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
