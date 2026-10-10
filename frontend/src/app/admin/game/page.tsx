"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { adminApi, AdminApiError } from "@/lib/adminApi";
import type { Game, QuizAdmin, TeamSummary } from "@/lib/adminTypes";
import type { QuizType } from "@/lib/types";
import TeamQrCode from "@/components/TeamQrCode";

export default function AdminGameDetailPage() {
  return (
    <Suspense fallback={<Centered>불러오는 중...</Centered>}>
      <GameDetail />
    </Suspense>
  );
}

const STATUS_LABELS: Record<Game["status"], string> = {
  READY: "대기",
  RUNNING: "진행 중",
  FINISHED: "종료",
};

function GameDetail() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const gameId = Number(searchParams.get("id"));

  const [game, setGame] = useState<Game | null>(null);
  const [teams, setTeams] = useState<TeamSummary[] | null>(null);
  const [quizzes, setQuizzes] = useState<QuizAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      await adminApi.me();
      const [games, allTeams, gameQuizzes] = await Promise.all([
        adminApi.listGames(),
        adminApi.listTeams(),
        adminApi.listQuizzes(gameId),
      ]);
      setGame(games.find((g) => g.id === gameId) ?? null);
      setTeams(allTeams.filter((t) => t.gameId === gameId));
      setQuizzes(gameQuizzes);
      setError(null);
    } catch (e) {
      if (e instanceof AdminApiError) {
        router.push("/admin/login");
        return;
      }
      setError("불러오지 못했습니다.");
    }
  }, [gameId, router]);

  useEffect(() => {
    load();
  }, [load]);

  if (error) return <Centered><p className="text-red-400">{error}</p></Centered>;
  if (!game || !teams || !quizzes) return <Centered>불러오는 중...</Centered>;

  return (
    <main className="flex-1 px-5 py-8 max-w-2xl mx-auto w-full space-y-8">
      <button onClick={() => router.push("/admin/games")} className="text-sm text-neutral-400">
        ← 게임 목록
      </button>

      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">{game.title}</h1>
          <p className="text-sm text-neutral-400">{STATUS_LABELS[game.status]}</p>
        </div>
        <div className="flex gap-2">
          {game.status === "READY" && (
            <button
              onClick={() => adminApi.startGame(gameId).then(load)}
              className="rounded-lg bg-emerald-600 px-3 py-2 text-sm font-semibold"
            >
              시작
            </button>
          )}
          {game.status === "RUNNING" && (
            <button
              onClick={() => adminApi.finishGame(gameId).then(load)}
              className="rounded-lg bg-amber-700 px-3 py-2 text-sm font-semibold"
            >
              종료
            </button>
          )}
        </div>
      </header>

      <TeamSection gameId={gameId} teams={teams} onChanged={load} />
      <QuizSection gameId={gameId} quizzes={quizzes} onChanged={load} />
    </main>
  );
}

function TeamSection({
  gameId,
  teams,
  onChanged,
}: {
  gameId: number;
  teams: TeamSummary[];
  onChanged: () => void;
}) {
  const [name, setName] = useState("");
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    setCreating(true);
    setError(null);
    try {
      await adminApi.createTeam(gameId, name.trim());
      setName("");
      onChanged();
    } catch (e) {
      setError(e instanceof AdminApiError ? e.message : "생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  }

  return (
    <section className="space-y-3">
      <h2 className="font-semibold">팀</h2>
      <form onSubmit={handleCreate} className="flex gap-2">
        <input
          className="flex-1 rounded-lg bg-neutral-900 border border-neutral-700 px-4 py-2"
          placeholder="새 팀 이름"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <button
          type="submit"
          disabled={creating || !name.trim()}
          className="rounded-lg bg-indigo-500 px-4 py-2 font-semibold disabled:opacity-40"
        >
          팀 추가
        </button>
      </form>
      {error && <p className="text-red-400 text-sm">{error}</p>}

      <div className="grid gap-4 sm:grid-cols-2">
        {teams.map((team) => (
          <div key={team.teamId} className="rounded-xl bg-neutral-900 border border-neutral-800 p-4 flex gap-4">
            <TeamQrCode inviteToken={team.inviteToken} />
            <div className="space-y-1 text-sm">
              <p className="font-medium">{team.teamName}</p>
              <p className="text-neutral-400">초대코드: {team.inviteToken}</p>
              <p className="text-neutral-500 text-xs">
                문제 {team.quizzes.quizzes.filter((q) => q.status === "COMPLETED").length}/
                {team.quizzes.quizzes.length} 완료
              </p>
            </div>
          </div>
        ))}
        {teams.length === 0 && <p className="text-neutral-500 text-sm">아직 팀이 없습니다.</p>}
      </div>
    </section>
  );
}

function QuizSection({
  gameId,
  quizzes,
  onChanged,
}: {
  gameId: number;
  quizzes: QuizAdmin[];
  onChanged: () => void;
}) {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [type, setType] = useState<QuizType>("TEXT");
  const [orderNo, setOrderNo] = useState(quizzes.length + 1);
  const [hint, setHint] = useState("");
  const [hintDelaySeconds, setHintDelaySeconds] = useState(0);
  const [answer, setAnswer] = useState("");
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;
    if (type === "TEXT" && !answer.trim()) {
      setError("TEXT 문제는 정답이 필요합니다.");
      return;
    }
    setCreating(true);
    setError(null);
    try {
      await adminApi.createQuiz(gameId, {
        title: title.trim(),
        content: content.trim(),
        type,
        orderNo,
        hint,
        hintDelaySeconds,
        answer: type === "TEXT" ? answer.trim() : undefined,
      });
      setTitle("");
      setContent("");
      setAnswer("");
      setOrderNo((n) => n + 1);
      onChanged();
    } catch (e) {
      setError(e instanceof AdminApiError ? e.message : "생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  }

  return (
    <section className="space-y-3">
      <h2 className="font-semibold">문제</h2>

      <form onSubmit={handleCreate} className="space-y-2 rounded-xl bg-neutral-900 border border-neutral-800 p-4">
        <input
          className="w-full rounded-lg bg-neutral-950 border border-neutral-700 px-3 py-2 text-sm"
          placeholder="문제 제목"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <textarea
          className="w-full rounded-lg bg-neutral-950 border border-neutral-700 px-3 py-2 text-sm"
          placeholder="문제 내용"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={2}
        />
        <div className="flex gap-2">
          <select
            className="rounded-lg bg-neutral-950 border border-neutral-700 px-3 py-2 text-sm"
            value={type}
            onChange={(e) => setType(e.target.value as QuizType)}
          >
            <option value="TEXT">TEXT (정답 입력)</option>
            <option value="IMAGE">IMAGE (사진 제출)</option>
          </select>
          <input
            type="number"
            className="w-20 rounded-lg bg-neutral-950 border border-neutral-700 px-3 py-2 text-sm"
            placeholder="순서"
            value={orderNo}
            onChange={(e) => setOrderNo(Number(e.target.value))}
          />
        </div>
        {type === "TEXT" && (
          <input
            className="w-full rounded-lg bg-neutral-950 border border-neutral-700 px-3 py-2 text-sm"
            placeholder="정답"
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
          />
        )}
        <div className="flex gap-2">
          <input
            className="flex-1 rounded-lg bg-neutral-950 border border-neutral-700 px-3 py-2 text-sm"
            placeholder="힌트 (선택)"
            value={hint}
            onChange={(e) => setHint(e.target.value)}
          />
          <input
            type="number"
            className="w-28 rounded-lg bg-neutral-950 border border-neutral-700 px-3 py-2 text-sm"
            placeholder="힌트 대기(초)"
            value={hintDelaySeconds}
            onChange={(e) => setHintDelaySeconds(Number(e.target.value))}
          />
        </div>
        {error && <p className="text-red-400 text-sm">{error}</p>}
        <button
          type="submit"
          disabled={creating}
          className="w-full rounded-lg bg-indigo-500 py-2 font-semibold disabled:opacity-40"
        >
          문제 추가
        </button>
      </form>

      <ul className="space-y-2">
        {quizzes.map((quiz) => (
          <li key={quiz.id} className="rounded-lg bg-neutral-900 border border-neutral-800 px-4 py-3 text-sm">
            <div className="flex items-center justify-between">
              <span className="font-medium">
                #{quiz.orderNo} {quiz.title}
              </span>
              <span className="text-xs text-neutral-500">{quiz.type}</span>
            </div>
            <p className="text-neutral-400 mt-1">{quiz.content}</p>
            {quiz.answer && <p className="text-neutral-500 text-xs mt-1">정답: {quiz.answer}</p>}
          </li>
        ))}
        {quizzes.length === 0 && <p className="text-neutral-500 text-sm">아직 문제가 없습니다.</p>}
      </ul>
    </section>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex-1 flex items-center justify-center">{children}</main>;
}
