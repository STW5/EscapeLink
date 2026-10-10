"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import type { LeaderboardResponse } from "@/lib/types";

const POLL_INTERVAL_MS = 5000;

export default function LeaderboardPage() {
  return (
    <Suspense fallback={<Centered>불러오는 중...</Centered>}>
      <LeaderboardView />
    </Suspense>
  );
}

function LeaderboardView() {
  const searchParams = useSearchParams();
  const gameId = searchParams.get("gameId");

  const [data, setData] = useState<LeaderboardResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const query = gameId ? `?gameId=${encodeURIComponent(gameId)}` : "";
      const result = await api.get<LeaderboardResponse>(`/api/leaderboard${query}`);
      setData(result);
      setError(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "불러오지 못했습니다.");
    }
  }, [gameId]);

  useEffect(() => {
    load();
    const id = setInterval(load, POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, [load]);

  if (error) {
    return <Centered><p className="text-red-400">{error}</p></Centered>;
  }

  if (!data) {
    return <Centered>불러오는 중...</Centered>;
  }

  return (
    <main className="flex-1 px-6 py-10 max-w-2xl mx-auto w-full space-y-6">
      <header className="text-center space-y-1">
        <h1 className="text-2xl font-bold">🏆 리더보드</h1>
        {data.gameTitle && <p className="text-neutral-400">{data.gameTitle}</p>}
      </header>

      {data.teams.length === 0 ? (
        <p className="text-center text-neutral-500 py-12">아직 참가 팀이 없습니다.</p>
      ) : (
        <ol className="space-y-2">
          {data.teams.map((team, index) => (
            <li
              key={team.teamName}
              className={`flex items-center justify-between rounded-xl border px-4 py-3 ${
                index === 0
                  ? "bg-amber-500/10 border-amber-500/40"
                  : "bg-neutral-900 border-neutral-800"
              }`}
            >
              <div className="flex items-center gap-3">
                <span className="w-7 text-right font-bold text-neutral-400">{index + 1}</span>
                <span className="font-semibold">{team.teamName}</span>
                {team.finalStageReached && (
                  <span className="text-xs px-2 py-0.5 rounded-full bg-indigo-900/60 text-indigo-300">
                    최종 스테이지
                  </span>
                )}
              </div>
              <div className="text-right text-sm">
                <p className="text-neutral-300">
                  {team.completedQuizCount}/{team.totalQuizCount} 완료
                </p>
                {team.clearTime && (
                  <p className="text-emerald-400 text-xs">
                    클리어 {new Date(team.clearTime).toLocaleTimeString("ko-KR")}
                  </p>
                )}
              </div>
            </li>
          ))}
        </ol>
      )}
    </main>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex-1 flex items-center justify-center">{children}</main>;
}
