import { useQuery } from '@tanstack/react-query';
import { airspacesService } from '../services/airspacesService';

/** Full airspace catalogue, optionally filtered by search query. */
export function useAirspaceList(q?: string) {
  return useQuery({
    queryKey: ['airspaces', q ?? ''],
    queryFn: () => airspacesService.getList(q),
    staleTime: 5 * 60_000,
  });
}

/** Single airspace detail (includes nearbyAirports) by cityId. */
export function useAirspaceDetail(cityId: string | null) {
  return useQuery({
    queryKey: ['airspace', cityId],
    queryFn: () => airspacesService.getDetail(cityId!),
    enabled: !!cityId,
    staleTime: 5 * 60_000,
    retry: 1,
  });
}
