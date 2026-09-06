import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { alertsService } from '../services/alertsService';
import { useAuthStore } from '../store/authStore';
import type { AlertDto } from '../types';

/**
 * Polls GET /api/alerts/unread-count every 30s per handoff §"Alerts" —
 * keeps the notification bell badge live without a full page refresh.
 * Also fires a browser Notification (Web Notifications API) for newly
 * observed unread alerts while the tab is open, per Section 43.
 */
export function useUnreadAlertCount() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  return useQuery({
    queryKey: ['alerts', 'unread-count'],
    queryFn: () => alertsService.getUnreadCount(),
    enabled: isAuthenticated,
    refetchInterval: 30_000,
    staleTime: 25_000,
  });
}

export function useAlertsList(enabled: boolean) {
  return useQuery({
    queryKey: ['alerts', 'list'],
    queryFn: () => alertsService.getAll(),
    enabled,
    refetchInterval: enabled ? 30_000 : false,
  });
}

export function useMarkAlertRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => alertsService.markRead(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['alerts'] });
    },
  });
}

export function useMarkAllAlertsRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => alertsService.markAllRead(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['alerts'] });
    },
  });
}

/** Requests browser notification permission once, on first authenticated mount. */
export function useBrowserNotificationPermission() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  useEffect(() => {
    if (!isAuthenticated) return;
    if (typeof Notification === 'undefined') return;
    if (Notification.permission === 'default') {
      Notification.requestPermission().catch(() => {});
    }
  }, [isAuthenticated]);
}

/** Fires a Web Notification for each newly-seen unread alert while the tab is open. */
export function useAlertBrowserNotifications(alerts: AlertDto[] | undefined) {
  const seenIds = useRef<Set<number>>(new Set());
  const initialized = useRef(false);

  useEffect(() => {
    if (!alerts) return;
    if (!initialized.current) {
      // Don't fire for alerts that already existed before this session started.
      for (const a of alerts) seenIds.current.add(a.id);
      initialized.current = true;
      return;
    }
    for (const a of alerts) {
      if (!a.read && !seenIds.current.has(a.id)) {
        seenIds.current.add(a.id);
        if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
          try {
            new Notification('AeroAI', { body: a.message });
          } catch {
            // Ignore — non-fatal.
          }
        }
      }
    }
  }, [alerts]);
}
