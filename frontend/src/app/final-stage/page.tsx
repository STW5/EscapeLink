"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import type { TeamState } from "@/lib/types";
import { useTeamChannel } from "@/lib/ws";

export default function FinalStagePage() {
  const router = useRouter();
  const [teamState, setTeamState] = useState<TeamState | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [clearing, setClearing] = useState(false);
  const [clearError, setClearError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const state = await api.get<TeamState>("/api/team-state");
      setTeamState(state);
      setLoadError(null);
    } catch (e) {
      if (e instanceof ApiError && e.code === "SESSION_EXPIRED") {
        router.push("/join");
        return;
      }
      setLoadError(e instanceof ApiError ? e.message : "불러오지 못했습니다.");
    }
  }, [router]);

  useEffect(() => {
    load();
  }, [load]);

  useTeamChannel(teamState?.teamId ?? null, () => {
    load();
  });

  async function handleClear() {
    setClearing(true);
    setClearError(null);
    try {
      const result = await api.post<{ clearedAt: string }>("/api/final-stage/clear");
      setTeamState((prev) => (prev ? { ...prev, finalStageClearedAt: result.clearedAt } : prev));
    } catch (e) {
      setClearError(e instanceof ApiError ? e.message : "처리에 실패했습니다.");
    } finally {
      setClearing(false);
    }
  }

  if (loadError) {
    return <Centered><p className="text-red-400">{loadError}</p></Centered>;
  }
  if (!teamState) {
    return <Centered>불러오는 중...</Centered>;
  }
  if (!teamState.finalStageUnlocked) {
    return (
      <Centered>
        <div className="text-center space-y-3">
          <p className="text-neutral-400">아직 최종 스테이지에 진입할 수 없습니다.</p>
          <button onClick={() => router.push("/quizzes")} className="text-indigo-400 text-sm">
            문제 목록으로
          </button>
        </div>
      </Centered>
    );
  }

  return (
    <main className="flex-1 flex flex-col items-center justify-center px-6 space-y-6 text-center">
      <h1 className="text-2xl font-bold">🏁 최종 미션</h1>

      {teamState.finalStageClearedAt ? (
        <div className="space-y-2">
          <p className="text-emerald-400 text-lg font-semibold">클리어 완료!</p>
          <p className="text-neutral-500 text-sm">
            {new Date(teamState.finalStageClearedAt).toLocaleString("ko-KR")}
          </p>
        </div>
      ) : (
        <div className="space-y-3 w-full max-w-sm">
          <p className="text-neutral-300">
            모든 미션을 완료했습니다. 현장 안내에 따라 최종 미션을 수행한 뒤 아래 버튼을 눌러주세요.
          </p>
          {clearError && <p className="text-red-400 text-sm">{clearError}</p>}
          <button
            onClick={handleClear}
            disabled={clearing}
            className="w-full rounded-lg bg-indigo-500 py-3 font-semibold disabled:opacity-40"
          >
            {clearing ? "처리 중..." : "클리어!"}
          </button>
        </div>
      )}

      <button onClick={() => router.push("/quizzes")} className="text-sm text-neutral-500">
        문제 목록으로
      </button>
    </main>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex-1 flex items-center justify-center">{children}</main>;
}
