import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useUnreadAlertCount,
  useAlertsList,
  useMarkAlertRead,
  useMarkAllAlertsRead,
  useAlertBrowserNotifications,
  useBrowserNotificationPermission,
} from '../../hooks/useAlerts';
import { formatTimestamp } from '../../utils/formatters';
import { LoadingSpinner } from '../common';
import type { AlertDto } from '../../types';

const ALERT_TYPE_LABEL: Record<AlertDto['type'], string> = {
  STATUS_CHANGE: 'Status change',
  DELAY_CATEGORY_CHANGE: 'Delay risk change',
  CONFLICT_RISK_CHANGE: 'Conflict risk change',
  DATA_UNAVAILABLE: 'Data unavailable',
};

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useBrowserNotificationPermission();

  const { data: unread } = useUnreadAlertCount();
  const { data: alerts, isLoading } = useAlertsList(open);
  useAlertBrowserNotifications(alerts);

  const markRead = useMarkAlertRead();
  const markAllRead = useMarkAllAlertsRead();

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const count = unread?.unreadCount ?? 0;

  function handleAlertClick(alert: AlertDto) {
    if (!alert.read) markRead.mutate(alert.id);
    setOpen(false);
    navigate('/my-flights');
  }

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((o) => !o)}
        className="relative flex items-center justify-center w-9 h-9 rounded-md hover:bg-aero-muted transition-colors"
        aria-label="Notifications"
      >
        <svg className="w-5 h-5 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8}
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {count > 0 && (
          <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-state-red text-white text-[10px] font-bold flex items-center justify-center">
            {count > 9 ? '9+' : count}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-80 max-h-[26rem] overflow-y-auto bg-aero-surface border border-aero-border rounded-lg shadow-2xl z-50 animate-fade-in">
          <div className="flex items-center justify-between px-4 py-3 border-b border-aero-border">
            <span className="text-sm font-semibold text-slate-200">Notifications</span>
            {alerts && alerts.some((a) => !a.read) && (
              <button
                onClick={() => markAllRead.mutate()}
                className="text-xs text-aero-cyan hover:underline"
              >
                Mark all as read
              </button>
            )}
          </div>

          {isLoading && <LoadingSpinner message="Loading…" className="p-4" size="sm" />}

          {!isLoading && (!alerts || alerts.length === 0) && (
            <div className="px-4 py-6 text-center text-sm text-state-gray">
              No notifications yet.
            </div>
          )}

          {!isLoading &&
            alerts?.map((a) => (
              <button
                key={a.id}
                onClick={() => handleAlertClick(a)}
                className={`w-full text-left px-4 py-3 border-b border-aero-border/60 hover:bg-aero-muted transition-colors ${
                  a.read ? 'opacity-60' : ''
                }`}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-semibold tracking-wide text-aero-cyan">
                    {ALERT_TYPE_LABEL[a.type] ?? a.type}
                  </span>
                  {!a.read && <span className="w-1.5 h-1.5 rounded-full bg-state-red shrink-0" />}
                </div>
                <p className="text-sm text-slate-200 mt-1">{a.message}</p>
                <p className="text-xs text-state-gray mt-1">{formatTimestamp(a.createdAt)}</p>
              </button>
            ))}
        </div>
      )}
    </div>
  );
}
