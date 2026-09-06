import { useEffect, useRef, useCallback } from 'react';
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { AircraftDto, AircraftDeltaMessage } from '../types';
import { flightsService } from '../services/flightsService';

const WS_URL = import.meta.env.VITE_WS_URL ?? 'http://localhost:8080/ws';

interface UseLiveAircraftOptions {
  cityId: string | null;
  onSnapshot: (aircraft: AircraftDto[]) => void;
  onDelta: (delta: AircraftDeltaMessage) => void;
  onConnectionChange: (state: 'connecting' | 'connected' | 'disconnected') => void;
}

/**
 * useWebSocket — manages STOMP/SockJS connection per handoff §T-Y.
 *
 * Procedure:
 *  1. Fetch REST snapshot via GET /api/flights/current?airspaceId={cityId}
 *  2. Open STOMP → SUBSCRIBE /topic/aircraft/{cityId}
 *  3. Apply delta messages
 *  4. On disconnect: show "Connection interrupted", retry with exponential backoff
 *  5. On reconnect: re-fetch snapshot THEN resubscribe
 */
export function useWebSocket({
  cityId,
  onSnapshot,
  onDelta,
  onConnectionChange,
}: UseLiveAircraftOptions) {
  const clientRef = useRef<Client | null>(null);
  const subscriptionRef = useRef<StompSubscription | null>(null);
  const retryTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const retryDelayRef = useRef(1000); // starts at 1s, doubles up to 30s cap
  const activeRef = useRef(true); // false when hook is cleaning up

  const clearRetry = useCallback(() => {
    if (retryTimeoutRef.current !== null) {
      clearTimeout(retryTimeoutRef.current);
      retryTimeoutRef.current = null;
    }
  }, []);

  const connect = useCallback(
    async (id: string) => {
      if (!activeRef.current) return;

      // 1. Fetch initial REST snapshot
      try {
        const snapshot = await flightsService.getCurrent(id);
        if (activeRef.current) onSnapshot(snapshot);
      } catch {
        // Non-fatal — proceed to WebSocket even if snapshot fails
        if (activeRef.current) onSnapshot([]);
      }

      if (!activeRef.current) return;

      // 2. Create STOMP client
      const client = new Client({
        // Use SockJS factory so it works in environments that block raw WS
        webSocketFactory: () => new SockJS(WS_URL) as unknown as WebSocket,
        reconnectDelay: 0, // We manage reconnect manually for precise backoff control
        onConnect: () => {
          if (!activeRef.current) return;
          retryDelayRef.current = 1000; // reset backoff on successful connect
          onConnectionChange('connected');

          // 3. Subscribe to city topic
          subscriptionRef.current = client.subscribe(
            `/topic/aircraft/${id}`,
            (message: IMessage) => {
              if (!activeRef.current) return;
              try {
                const delta = JSON.parse(message.body) as AircraftDeltaMessage;
                onDelta(delta);
              } catch {
                // Malformed message — ignore
              }
            }
          );
        },
        onWebSocketClose: () => {
          if (!activeRef.current) return;
          subscriptionRef.current = null;
          onConnectionChange('disconnected');

          // 4. Exponential backoff reconnect
          const delay = retryDelayRef.current;
          retryDelayRef.current = Math.min(delay * 2, 30000);
          retryTimeoutRef.current = setTimeout(async () => {
            if (!activeRef.current) return;
            onConnectionChange('connecting');
            // Re-fetch snapshot to close any gap, then reconnect
            try {
              const snapshot = await flightsService.getCurrent(id);
              if (activeRef.current) onSnapshot(snapshot);
            } catch {
              // Non-fatal
            }
            if (activeRef.current) {
              client.activate();
            }
          }, delay);
        },
        onStompError: () => {
          // STOMP protocol error — disconnect will follow
        },
      });

      clientRef.current = client;
      onConnectionChange('connecting');
      client.activate();
    },
    [onSnapshot, onDelta, onConnectionChange]
  );

  const disconnect = useCallback(() => {
    clearRetry();
    subscriptionRef.current?.unsubscribe();
    subscriptionRef.current = null;
    if (clientRef.current?.active) {
      clientRef.current.deactivate();
    }
    clientRef.current = null;
  }, [clearRetry]);

  useEffect(() => {
    activeRef.current = true;
    if (!cityId) return;

    connect(cityId);

    return () => {
      activeRef.current = false;
      disconnect();
    };
  }, [cityId, connect, disconnect]);
}
