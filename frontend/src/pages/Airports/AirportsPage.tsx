import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { airportsService } from '../../services/airportsService';
import { LoadingSpinner, ErrorState, EmptyState } from '../../components/common';
import { extractErrorMessage } from '../../api/client';

function useDebounced(value: string, delay: number): string {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

export function AirportsPage() {
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const navigate = useNavigate();
  const debounced = useDebounced(query, 300);
  const isSearching = debounced.trim().length > 0;

  const {
    data: searchResults,
    isLoading: searchLoading,
    isError: searchError,
    error: searchErrorObj,
    refetch: refetchSearch,
  } = useQuery({
    queryKey: ['airports-search', debounced],
    queryFn: () => airportsService.search(debounced.trim()),
    enabled: isSearching,
  });

  const {
    data: pageData,
    isLoading: pageLoading,
    isError: pageError,
    error: pageErrorObj,
    refetch: refetchPage,
  } = useQuery({
    queryKey: ['airports-page', page],
    queryFn: () => airportsService.getPage(page, 50),
    enabled: !isSearching,
  });

  const isLoading = isSearching ? searchLoading : pageLoading;
  const isError = isSearching ? searchError : pageError;
  const errorObj = isSearching ? searchErrorObj : pageErrorObj;
  const refetch = isSearching ? refetchSearch : refetchPage;
  const airports = isSearching ? searchResults ?? [] : pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 1;

  return (
    <div className="max-w-6xl mx-auto px-4 md:px-8 py-6">
      <h1 className="text-xl font-semibold text-slate-100 mb-1">Airports</h1>
      <p className="text-sm text-state-gray mb-5">Search the global airport directory.</p>

      <div className="panel p-4 mb-5">
        <input
          value={query}
          onChange={(e) => { setQuery(e.target.value); setPage(0); }}
          placeholder="Search by name, IATA, ICAO, or city…"
          className="aero-input"
        />
      </div>

      <div className="panel p-4">
        {isLoading && <LoadingSpinner message="Loading airports…" className="p-8 justify-center" />}
        {isError && <ErrorState message={extractErrorMessage(errorObj) || 'Could not load airports.'} onRetry={() => refetch()} />}
        {!isLoading && !isError && airports.length === 0 && (
          <EmptyState message="No matching flights or airports found." />
        )}
        {!isLoading && !isError && airports.length > 0 && (
          <>
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {airports.map((a) => (
                <button
                  key={a.id}
                  onClick={() => navigate(`/airports/${a.id}`)}
                  className="text-left p-4 rounded-md border border-aero-border bg-aero-muted/40 hover:bg-aero-muted hover:border-aero-cyan/40 transition-colors"
                >
                  <p className="text-sm font-medium text-slate-100 mb-1">{a.name}</p>
                  <p className="text-xs text-state-gray">
                    {a.iata ?? '—'} / {a.icao ?? '—'} · {a.city ?? 'Unavailable'}, {a.country ?? 'Unavailable'}
                  </p>
                </button>
              ))}
            </div>

            {!isSearching && totalPages > 1 && (
              <div className="flex items-center justify-between mt-4 pt-4 border-t border-aero-border">
                <span className="text-xs text-state-gray">
                  Page {page + 1} of {totalPages}
                </span>
                <div className="flex gap-2">
                  <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0} className="btn-secondary text-xs px-3 py-1.5 disabled:opacity-40">
                    Previous
                  </button>
                  <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="btn-secondary text-xs px-3 py-1.5 disabled:opacity-40">
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
