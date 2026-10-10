"use client";

import { useState } from "react";
import { adminApi, AdminApiError } from "@/lib/adminApi";
import type { TeamSummary } from "@/lib/adminTypes";
import StatusBadge from "@/components/StatusBadge";

export default function TeamControlCard({
  team,
  onChanged,
}: {
  team: TeamSummary;
  onChanged: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      onChanged();
    } catch (e) {
      setError(e instanceof AdminApiError ? e.message : "처리에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  }

  function handleForceComplete(quizId: number) {
    if (!window.confirm("이 문제를 강제로 통과 처리할까요?")) return;
    run(() => adminApi.forceCompleteQuiz(team.teamId, quizId));
  }

  function handleReset() {
    if (!window.confirm(`${team.teamName}을 초기화할까요? (진행 상태가 전부 미해결로 리셋됩니다)`)) return;
    run(() => adminApi.resetTeam(team.teamId));
  }

  function handleForceFinalStage() {
    if (!window.confirm(`${team.teamName}을 최종 스테이지로 강제 진입시킬까요?`)) return;
    run(() => adminApi.forceFinalStage(team.teamId));
  }

  return (
    <div className="rounded-xl bg-neutral-900 border border-neutral-800 p-4 space-y-3">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="font-semibold">{team.teamName}</h2>
          <p className="text-xs text-neutral-500">
            초대코드: {team.inviteToken} · run #{team.currentRunNo}
          </p>
        </div>
        {team.finalStageUnlocked && (
          <span className="text-xs px-2.5 py-1 rounded-full bg-indigo-900/60 text-indigo-300">
            최종 스테이지{team.finalStageForced ? " (강제)" : ""}
          </span>
        )}
      </div>

      <ul className="space-y-1.5">
        {team.quizzes.quizzes.map((quiz) => (
          <li key={quiz.id} className="flex items-center justify-between text-sm">
            <span className="text-neutral-300">{quiz.title}</span>
            <div className="flex items-center gap-2">
              <StatusBadge status={quiz.status} />
              {quiz.status !== "COMPLETED" && (
                <button
                  onClick={() => handleForceComplete(quiz.id)}
                  disabled={busy}
                  className="text-xs px-2 py-1 rounded bg-neutral-800 border border-neutral-700 disabled:opacity-40"
                >
                  강제 통과
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>

      {error && <p className="text-red-400 text-sm">{error}</p>}

      <div className="flex gap-2 pt-1">
        <button
          onClick={handleReset}
          disabled={busy}
          className="flex-1 rounded-lg bg-amber-700 py-2 text-sm font-semibold disabled:opacity-40"
        >
          팀 초기화
        </button>
        <button
          onClick={handleForceFinalStage}
          disabled={busy || team.finalStageUnlocked}
          className="flex-1 rounded-lg bg-indigo-600 py-2 text-sm font-semibold disabled:opacity-40"
        >
          최종 스테이지 강제 진입
        </button>
      </div>
    </div>
  );
}
