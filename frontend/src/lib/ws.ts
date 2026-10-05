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
  onTeamEvent: (message: TeamEvent) => void
) {
  const handlerRef = useRef(onTeamEvent);
  useEffect(() => {
    handlerRef.current = onTeamEvent;
  });

  useEffect(() => {
    if (teamId == null) {
      return;
    }

    const client = new Client({
      webSocketFactory: () =>
        new SockJS(`${API_BASE_URL}/ws`) as unknown as IStompSocket,
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
      },
    });

    client.activate();
    return () => {
      client.deactivate();
    };
  }, [teamId]);
}
