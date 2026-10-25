"use client";

import { useEffect, useRef } from "react";
import { Client, type IMessage, type IStompSocket } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { API_BASE_URL } from "./api";
import type { TeamEvent } from "./types";

/**
 * Subscribes to this team's own broadcast channel for the lifetime of the
 * component. The handshake authenticates via the same TeamSession cookie
 * used for REST calls, so a socket can never be opened for another team.
 */
export function useTeamChannel(
  teamId: number | null,
  onTeamEvent: (message: TeamEvent) => void,
  onConnected?: () => void
) {
  const handlerRef = useRef(onTeamEvent);
  const connectedRef = useRef(onConnected);
  useEffect(() => {
    handlerRef.current = onTeamEvent;
    connectedRef.current = onConnected;
  });

  useEffect(() => {
    if (teamId == null) {
      return;
    }

    const webSocketBaseUrl = API_BASE_URL || window.location.origin;
    const client = new Client({
      webSocketFactory: () =>
        new SockJS(new URL("/ws", webSocketBaseUrl).toString()) as unknown as IStompSocket,
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe(`/topic/teams/${teamId}`, (message: IMessage) => {
          try {
            const payload = JSON.parse(message.body) as TeamEvent;
            handlerRef.current(payload);
          } catch {
            // ignore malformed payloads
          }
        });
        // WebSocket events are notify-only and may be missed while disconnected.
        // Every initial connection/reconnection therefore reloads the DB snapshot.
        connectedRef.current?.();
      },
    });

    client.activate();
    return () => {
      client.deactivate();
    };
  }, [teamId]);
}

/**
 * Admin console variant: subscribes to the admin-only topic, authenticated by
 * the admin login session. Events only say "something changed" — callers
 * refetch the authoritative snapshot, both on events and on (re)connect.
 */
export function useAdminChannel(onChange: () => void) {
  const handlerRef = useRef(onChange);
  useEffect(() => {
    handlerRef.current = onChange;
  });

  useEffect(() => {
    const webSocketBaseUrl = API_BASE_URL || window.location.origin;
    let timer: ReturnType<typeof setTimeout> | null = null;
    // Bursts of events (e.g. several teams finishing together) collapse into one refetch.
    const notify = () => {
      if (timer) return;
      timer = setTimeout(() => {
        timer = null;
        handlerRef.current();
      }, 300);
    };

    const client = new Client({
      webSocketFactory: () =>
        new SockJS(new URL("/ws", webSocketBaseUrl).toString()) as unknown as IStompSocket,
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe("/topic/admin", notify);
        notify();
      },
    });

    client.activate();
    return () => {
      if (timer) clearTimeout(timer);
      client.deactivate();
    };
  }, []);
}
