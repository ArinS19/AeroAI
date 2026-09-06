interface LoadingSpinnerProps {
  message?: string;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const SIZE_CLASSES = { sm: 'w-4 h-4', md: 'w-6 h-6', lg: 'w-8 h-8' };

export function LoadingSpinner({
  message = 'Loading…',
  size = 'md',
  className = '',
}: LoadingSpinnerProps) {
  return (
    <div className={`flex items-center gap-3 text-slate-400 ${className}`}>
      <svg
        className={`${SIZE_CLASSES[size]} animate-spin text-aero-cyan`}
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle
          className="opacity-25"
          cx="12" cy="12" r="10"
          stroke="currentColor" strokeWidth="4"
        />
        <path
          className="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
        />
      </svg>
      {message && <span className="text-sm">{message}</span>}
    </div>
  );
}
