"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { useTeamChannel } from "@/lib/ws";
import type { QuizList, TeamState } from "@/lib/types";
import GameTimer from "@/components/GameTimer";
import StatusBadge from "@/components/StatusBadge";

export default function QuizzesPage() {
  const router = useRouter();
  const [teamState, setTeamState] = useState<TeamState | null>(null);
  const [quizList, setQuizList] = useState<QuizList | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const loadAll = useCallback(async () => {
    try {
      const [state, list] = await Promise.all([
        api.get<TeamState>("/api/team-state"),
        api.get<QuizList>("/api/quizzes"),
      ]);
      setTeamState(state);
      setQuizList(list);
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
    loadAll();
  }, [loadAll]);

  // WebSocket is notify-only: on any completion event, just re-fetch the
  // authoritative snapshot instead of trying to patch local state.
  useTeamChannel(teamState?.teamId ?? null, () => {
    loadAll();
  });

  if (loadError) {
    return (
      <Centered>
        <p className="text-red-400">{loadError}</p>
      </Centered>
    );
  }

  if (!teamState || !quizList) {
    return <Centered>불러오는 중...</Centered>;
  }

  return (
    <main className="flex-1 px-5 py-8 max-w-md mx-auto w-full space-y-6">
      <header className="text-center space-y-1">
        <p className="text-neutral-400 text-sm">{teamState.teamName}</p>
        <GameTimer endAt={teamState.gameEndAt} status={teamState.gameStatus} />
      </header>

      <ul className="space-y-3">
        {quizList.quizzes.map((quiz) => (
          <li key={quiz.id}>
            <button
              onClick={() => router.push(`/quiz?id=${quiz.id}`)}
              className="w-full flex items-center justify-between rounded-xl bg-neutral-900 border border-neutral-800 px-4 py-4 text-left"
            >
              <span className="font-medium">{quiz.title}</span>
              <StatusBadge status={quiz.status} />
            </button>
          </li>
        ))}
      </ul>
    </main>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return (
    <main className="flex-1 flex items-center justify-center">{children}</main>
  );
}
