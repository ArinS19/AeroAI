import React from 'react';

interface EmptyStateProps {
  message: string;
  icon?: React.ReactNode;
  className?: string;
}

export function EmptyState({ message, icon, className = '' }: EmptyStateProps) {
  return (
    <div className={`flex flex-col items-center gap-3 p-8 text-center ${className}`}>
      {icon ?? (
        <svg className="w-10 h-10 text-state-gray" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      )}
      <p className="text-sm text-state-gray">{message}</p>
    </div>
  );
}
