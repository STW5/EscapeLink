"use client";

import { useEffect, useState } from "react";
import type { GameStatus } from "@/lib/types";

/**
 * Countdown is derived from the server-provided endAt on every render; the
 * effect only subscribes to a 1s tick, it never computes/sets the remaining
 * time itself. No per-second polling of the backend, per the timer policy.
 */
export default function GameTimer({
  endAt,
  status,
}: {
  endAt: string | null;
  status: GameStatus;
}) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  if (status === "READY") {
    return <p className="text-lg font-semibold text-neutral-400">대기 중</p>;
  }

  if (status === "FINISHED") {
    return <p className="text-lg font-semibold text-neutral-400">게임 종료</p>;
  }

  // Running without an end time: started, just no countdown to show.
  if (!endAt) {
    return <p className="text-lg font-semibold text-emerald-400">진행 중</p>;
  }

  const remainingMs = Math.max(new Date(endAt).getTime() - now, 0);

  if (remainingMs <= 0) {
    return <p className="text-lg font-semibold text-neutral-400">게임 종료</p>;
  }

  const totalSeconds = Math.floor(remainingMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  return (
    <p className="text-2xl font-bold tabular-nums">
      {minutes.toString().padStart(2, "0")}:{seconds.toString().padStart(2, "0")}
    </p>
  );
}
