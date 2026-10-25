"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { useTeamChannel } from "@/lib/ws";
import { useRefreshOnResume } from "@/lib/connectivity";
import type { QuizList, TeamEvent, TeamState } from "@/lib/types";
import GameTimer from "@/components/GameTimer";
import StatusBadge from "@/components/StatusBadge";

export default function QuizzesPage() {
  const router = useRouter();
  const [teamState, setTeamState] = useState<TeamState | null>(null);
  const [quizList, setQuizList] = useState<QuizList | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const quizListRef = useRef<QuizList | null>(null);

  const loadAll = useCallback(async () => {
    try {
      const [state, list] = await Promise.all([
        api.get<TeamState>("/api/team-state"),
        api.get<QuizList>("/api/quizzes"),
      ]);
      setTeamState(state);
      setQuizList(list);
      quizListRef.current = list;
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

  useRefreshOnResume(loadAll);

  // WebSocket is notify-only: on any completion event, just re-fetch the
  // authoritative snapshot instead of trying to patch local state.
  const handleTeamEvent = useCallback(
    (event: TeamEvent) => {
      const quizId = "quizId" in event ? event.quizId : null;
      const title = quizListRef.current?.quizzes.find((q) => q.id === quizId)?.title;
      const message = describeEvent(event.type, title);
      if (message) setToast(message);
      loadAll();
    },
    [loadAll]
  );
  useTeamChannel(teamState?.teamId ?? null, handleTeamEvent, loadAll);

  useEffect(() => {
    if (!toast) return;
    const id = setTimeout(() => setToast(null), 4500);
    return () => clearTimeout(id);
  }, [toast]);

  if (loadError) {
    return (
      <Centered>
        <div className="text-center space-y-3 px-6">
          <p className="text-red-400">{loadError}</p>
          <button onClick={loadAll} className="rounded-lg bg-neutral-800 border border-neutral-700 px-4 py-2 text-sm">
            다시 시도
          </button>
        </div>
      </Centered>
    );
  }

  if (!teamState || !quizList) {
    return <Centered>불러오는 중...</Centered>;
  }

  const total = quizList.quizzes.length;
  const completed = quizList.quizzes.filter((q) => q.status === "COMPLETED").length;
  const playable = teamState.gameStatus === "RUNNING";

  return (
    <main className="flex-1 px-5 py-8 max-w-md mx-auto w-full space-y-6">
      {toast && (
        <div
          role="status"
          className="fixed left-1/2 top-4 z-40 w-[calc(100%-2.5rem)] max-w-sm -translate-x-1/2 rounded-xl bg-neutral-800 border border-neutral-600 px-4 py-3 text-sm shadow-lg"
        >
          {toast}
        </div>
      )}

      <header className="text-center space-y-1">
        <p className="text-neutral-400 text-sm">{teamState.teamName}</p>
        <GameTimer endAt={teamState.gameEndAt} status={teamState.gameStatus} />
      </header>

      {!playable && (
        <p className="rounded-xl bg-neutral-900 border border-neutral-800 px-4 py-3 text-center text-sm text-neutral-300">
          {teamState.gameStatus === "READY"
            ? "게임이 아직 시작되지 않았어요. 시작되면 이 화면에서 바로 문제를 풀 수 있어요."
            : "게임이 종료되었어요. 수고하셨습니다!"}
        </p>
      )}

      {total > 0 && (
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-xs text-neutral-400">
            <span>진행 상황</span>
            <span>{completed} / {total} 완료</span>
          </div>
          <div className="h-2 rounded-full bg-neutral-800 overflow-hidden" role="progressbar" aria-valuemin={0} aria-valuemax={total} aria-valuenow={completed}>
            <div className="h-full bg-emerald-500 transition-all" style={{ width: `${(completed / total) * 100}%` }} />
          </div>
        </div>
      )}

      {teamState.finalStageUnlocked && (
        <button
          onClick={() => router.push("/final-stage")}
          className="w-full rounded-xl bg-indigo-600 px-4 py-4 text-left font-semibold"
        >
          🏁 최종 미션으로 이동
          {teamState.finalStageClearedAt && (
            <span className="block text-xs font-normal text-indigo-200 mt-1">클리어 완료</span>
          )}
        </button>
      )}

      <ul className="space-y-3">
        {quizList.quizzes.map((quiz, index) => (
          <li key={quiz.id}>
            <button
              onClick={() => router.push(`/quiz?id=${quiz.id}`)}
              className="w-full flex items-center justify-between rounded-xl bg-neutral-900 border border-neutral-800 px-4 py-4 text-left"
            >
              <span className="font-medium">
                <span className="mr-2 text-neutral-500">{index + 1}</span>
                {quiz.title}
              </span>
              <StatusBadge status={quiz.status} />
            </button>
          </li>
        ))}
      </ul>
    </main>
  );
}

function describeEvent(type: TeamEvent["type"], title?: string): string | null {
  const name = title ? `'${title}' ` : "";
  switch (type) {
    case "QUIZ_COMPLETED":
      return `${name}문제가 완료되었어요.`;
    case "QUIZ_FORCE_COMPLETED":
      return `${name}문제가 운영진에 의해 통과 처리되었어요.`;
    case "IMAGE_APPROVED":
      return `${name}사진이 승인되었어요! 🎉`;
    case "IMAGE_REJECTED":
      return `${name}사진이 반려되었어요. 다시 제출해 주세요.`;
    case "FINAL_STAGE_ENABLED":
    case "FORCE_FINAL_STAGE":
      return "최종 미션이 열렸어요!";
    case "GAME_STARTED":
      return "게임이 시작되었어요! 문제를 풀어보세요 🔥";
    case "GAME_FINISHED":
      return "게임이 종료되었어요. 수고하셨습니다!";
    case "TEAM_RESET":
      return "팀 진행 상황이 초기화되었어요.";
    default:
      return null;
  }
}

function Centered({ children }: { children: React.ReactNode }) {
  return (
    <main className="flex-1 flex items-center justify-center">{children}</main>
  );
}
