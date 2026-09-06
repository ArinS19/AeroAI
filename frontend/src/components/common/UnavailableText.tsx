

/**
 * Renders the literal word "Unavailable" in muted italic gray.
 * Per §3.1: always use this component, never render 0 / -- / empty for null fields.
 */
export function UnavailableText({ className = '' }: { className?: string }) {
  return (
    <span className={`text-state-gray italic text-sm ${className}`}>Unavailable</span>
  );
}
