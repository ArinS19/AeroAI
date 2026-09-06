import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="min-h-full flex flex-col items-center justify-center gap-4 px-4 py-24 text-center">
      <p className="text-6xl font-bold text-aero-cyan/40 font-mono">404</p>
      <h1 className="text-xl font-semibold text-slate-100">Page not found</h1>
      <p className="text-sm text-state-gray max-w-sm">
        The page you're looking for doesn't exist or has moved.
      </p>
      <Link to="/" className="btn-primary mt-2">
        Back to home
      </Link>
    </div>
  );
}
