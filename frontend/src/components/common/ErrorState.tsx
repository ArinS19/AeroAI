interface ErrorStateProps {
  message: string;
  onRetry?: () => void;
  className?: string;
}

export function ErrorState({ message, onRetry, className = '' }: ErrorStateProps) {
  return (
    <div className={`flex flex-col items-center gap-3 p-6 text-center ${className}`}>
      <div className="w-10 h-10 rounded-full bg-state-red/20 flex items-center justify-center">
        <svg className="w-5 h-5 text-state-red" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <p className="text-sm text-slate-400">{message}</p>
      {onRetry && (
        <button onClick={onRetry} className="btn-secondary text-xs px-3 py-1.5">
          Retry
        </button>
      )}
    </div>
  );
}
