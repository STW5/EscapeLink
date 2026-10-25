"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { adminApi, AdminApiError } from "@/lib/adminApi";
import type { Game, QuizAdmin, TeamSummary } from "@/lib/adminTypes";
import type { QuizType } from "@/lib/types";
import { useAdminChannel } from "@/lib/ws";
import { useConfirm } from "@/lib/useConfirm";
import AdminNav from "@/components/AdminNav";
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

const INPUT = "rounded-lg bg-neutral-950 border border-neutral-700 px-3 py-2 text-sm";
const BUTTON_SUBTLE = "rounded-lg bg-neutral-800 border border-neutral-700 px-3 py-1.5 text-xs disabled:opacity-40";

/** ISO instant -> value for <input type="datetime-local"> in the browser's timezone. */
function toLocalInput(iso: string | null): string {
  if (!iso) return "";
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function errorMessage(e: unknown, fallback: string) {
  return e instanceof AdminApiError ? e.message : fallback;
}

function GameDetail() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const gameId = Number(searchParams.get("id"));

  const [game, setGame] = useState<Game | null>(null);
  const [teams, setTeams] = useState<TeamSummary[] | null>(null);
  const [quizzes, setQuizzes] = useState<QuizAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  const load = useCallback(async () => {
    try {
      await adminApi.me();
      const [games, allTeams, gameQuizzes] = await Promise.all([
        adminApi.listGames(),
        adminApi.listTeams(),
        adminApi.listQuizzes(gameId),
      ]);
      const found = games.find((g) => g.id === gameId) ?? null;
      setGame(found);
      setNotFound(found === null);
      setTeams(allTeams.filter((t) => t.gameId === gameId));
      setQuizzes(gameQuizzes);
      setError(null);
    } catch (e) {
      if (e instanceof AdminApiError && e.code !== "GAME_NOT_FOUND") {
        router.push("/admin/login");
        return;
      }
      setError("불러오지 못했습니다.");
    }
  }, [gameId, router]);

  useEffect(() => {
    load();
  }, [load]);

  // Live updates: progress, edits made from another device, new teams/quizzes.
  useAdminChannel(load);

  if (error) return <Centered><p className="text-red-400">{error}</p></Centered>;
  if (notFound) {
    return (
      <Centered>
        <div className="text-center space-y-3">
          <p className="text-neutral-400">게임을 찾을 수 없습니다. 삭제되었을 수 있습니다.</p>
          <a href="/admin/games" className="text-sm text-indigo-400">게임 목록으로</a>
        </div>
      </Centered>
    );
  }
  if (!game || !teams || !quizzes) return <Centered>불러오는 중...</Centered>;

  return (
    <main className="flex-1 px-5 py-6 max-w-2xl mx-auto w-full space-y-6">
      <AdminNav />
      <GameHeader game={game} onChanged={load} onDeleted={() => router.push("/admin/games")} />
      <TeamSection gameId={gameId} gameRunning={game.status === "RUNNING"} teams={teams} onChanged={load} />
      <QuizSection gameId={gameId} quizzes={quizzes} onChanged={load} />
    </main>
  );
}

function GameHeader({
  game,
  onChanged,
  onDeleted,
}: {
  game: Game;
  onChanged: () => void;
  onDeleted: () => void;
}) {
  const [confirm, dialog] = useConfirm();
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState(game.title);
  const [endAt, setEndAt] = useState(toLocalInput(game.endAt));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function run(action: () => Promise<unknown>, fallback: string) {
    setBusy(true);
    setError(null);
    try {
      await action();
      return true;
    } catch (e) {
      setError(errorMessage(e, fallback));
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    const ok = await run(
      () =>
        adminApi.updateGame(game.id, {
          title: title.trim(),
          startAt: game.startAt,
          endAt: endAt ? new Date(endAt).toISOString() : null,
        }),
      "수정에 실패했습니다."
    );
    if (ok) {
      setEditing(false);
      onChanged();
    }
  }

  async function handleDelete() {
    if (!(await confirm(`'${game.title}' 게임을 삭제할까요?\n팀, 문제, 진행 기록이 모두 삭제되며 되돌릴 수 없습니다.`, { confirmLabel: "삭제", danger: true }))) return;
    if (await run(() => adminApi.deleteGame(game.id), "삭제에 실패했습니다.")) onDeleted();
  }

  async function handleStart() {
    if (!game.endAt) {
      const ok = await confirm("종료 시각이 설정되지 않았습니다. 참가자 화면에 남은 시간이 표시되지 않아요.\n그래도 시작할까요?", { confirmLabel: "시작" });
      if (!ok) return;
    } else if (!(await confirm("게임을 시작할까요? 참가자들이 바로 문제를 풀 수 있게 됩니다.", { confirmLabel: "시작" }))) {
      return;
    }
    if (await run(() => adminApi.startGame(game.id), "시작에 실패했습니다.")) onChanged();
  }

  async function handleFinish() {
    if (!(await confirm("게임을 종료할까요? 종료 후에는 참가자가 답을 제출할 수 없습니다.", { confirmLabel: "종료", danger: true }))) return;
    if (await run(() => adminApi.finishGame(game.id), "종료에 실패했습니다.")) onChanged();
  }

  return (
    <header className="space-y-2">
      {dialog}
      <a href="/admin/games" className="text-sm text-neutral-500">← 게임 목록</a>
      <div className="flex items-start justify-between gap-3">
        {editing ? (
          <form onSubmit={handleSave} className="flex-1 space-y-2">
            <input className={`${INPUT} w-full`} value={title} onChange={(e) => setTitle(e.target.value)} autoFocus />
            <div className="space-y-1.5">
              <label className="flex items-center gap-2 text-xs text-neutral-400">
                종료 시각
                <input
                  type="datetime-local"
                  className={`${INPUT} flex-1`}
                  value={endAt}
                  onChange={(e) => setEndAt(e.target.value)}
                />
              </label>
              <div className="flex flex-wrap gap-2 text-xs">
                {[60, 90, 120].map((minutes) => (
                  <button key={minutes} type="button" onClick={() => setEndAt(toLocalInput(new Date(Date.now() + minutes * 60000).toISOString()))} className={BUTTON_SUBTLE}>
                    지금부터 {minutes}분
                  </button>
                ))}
                {endAt && (
                  <button type="button" onClick={() => setEndAt("")} className={BUTTON_SUBTLE}>
                    시각 지우기
                  </button>
                )}
              </div>
            </div>
            <div className="flex gap-2">
              <button type="submit" disabled={busy || !title.trim()} className="rounded-lg bg-indigo-500 px-4 py-1.5 text-sm font-semibold disabled:opacity-40">
                저장
              </button>
              <button type="button" onClick={() => { setEditing(false); setTitle(game.title); setEndAt(toLocalInput(game.endAt)); }} className={BUTTON_SUBTLE}>
                취소
              </button>
            </div>
          </form>
        ) : (
          <div>
            <h1 className="text-xl font-bold">{game.title}</h1>
            <p className="text-sm text-neutral-400">
              {STATUS_LABELS[game.status]}
              <span className="text-neutral-500">
                {" · "}
                {game.endAt ? `종료 ${new Date(game.endAt).toLocaleString("ko-KR", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" })}` : "종료 시각 미설정"}
              </span>
            </p>
          </div>
        )}
        <div className="flex gap-2 shrink-0">
          {game.status === "READY" && (
            <button onClick={handleStart} disabled={busy} className="rounded-lg bg-emerald-600 px-3 py-2 text-sm font-semibold disabled:opacity-40">
              시작
            </button>
          )}
          {game.status === "RUNNING" && (
            <button onClick={handleFinish} disabled={busy} className="rounded-lg bg-amber-700 px-3 py-2 text-sm font-semibold disabled:opacity-40">
              종료
            </button>
          )}
        </div>
      </div>
      {!editing && (
        <div className="flex gap-2">
          <button onClick={() => setEditing(true)} className={BUTTON_SUBTLE}>이름·종료 시각 수정</button>
          <button
            onClick={handleDelete}
            disabled={busy || game.status === "RUNNING"}
            title={game.status === "RUNNING" ? "진행 중인 게임은 종료 후 삭제할 수 있습니다." : undefined}
            className={`${BUTTON_SUBTLE} text-red-400`}
          >
            게임 삭제
          </button>
        </div>
      )}
      {error && <p className="text-red-400 text-sm">{error}</p>}
    </header>
  );
}

function TeamSection({
  gameId,
  gameRunning,
  teams,
  onChanged,
}: {
  gameId: number;
  gameRunning: boolean;
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
      setError(errorMessage(e, "생성에 실패했습니다."));
    } finally {
      setCreating(false);
    }
  }

  return (
    <section className="space-y-3">
      <h2 className="font-semibold">팀 <span className="text-neutral-500 font-normal">({teams.length})</span></h2>
      <form onSubmit={handleCreate} className="flex gap-2">
        <input
          className={`${INPUT} flex-1`}
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
          <TeamCard key={team.teamId} team={team} gameRunning={gameRunning} onChanged={onChanged} />
        ))}
        {teams.length === 0 && <p className="text-neutral-500 text-sm">아직 팀이 없습니다.</p>}
      </div>
    </section>
  );
}

function TeamCard({
  team,
  gameRunning,
  onChanged,
}: {
  team: TeamSummary;
  gameRunning: boolean;
  onChanged: () => void;
}) {
  const [confirm, dialog] = useConfirm();
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(team.teamName);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const completed = team.quizzes.quizzes.filter((q) => q.status === "COMPLETED").length;
  const total = team.quizzes.quizzes.length;

  async function run(action: () => Promise<unknown>, fallback: string) {
    setBusy(true);
    setError(null);
    try {
      await action();
      onChanged();
      return true;
    } catch (e) {
      setError(errorMessage(e, fallback));
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function handleRename(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    if (await run(() => adminApi.renameTeam(team.teamId, name.trim()), "이름 변경에 실패했습니다.")) {
      setEditing(false);
    }
  }

  async function handleRegenerate() {
    if (!(await confirm(`'${team.teamName}' 팀의 초대코드를 다시 발급할까요?\n기존 QR 코드는 더 이상 사용할 수 없습니다. (이미 접속한 기기는 유지됩니다)`, { confirmLabel: "재발급" }))) return;
    run(() => adminApi.regenerateInvite(team.teamId), "재발급에 실패했습니다.");
  }

  async function handleDelete() {
    const warning = completed > 0 || team.finalStageUnlocked
      ? "\n진행 기록(풀이 상태, 제출 이미지)도 함께 삭제됩니다."
      : "";
    if (!(await confirm(`'${team.teamName}' 팀을 삭제할까요?${warning}\n되돌릴 수 없습니다.`, { confirmLabel: "삭제", danger: true }))) return;
    run(() => adminApi.deleteTeam(team.teamId), "삭제에 실패했습니다.");
  }

  return (
    <div className="rounded-xl bg-neutral-900 border border-neutral-800 p-4 space-y-3">
      {dialog}
      <div className="flex gap-4">
        <TeamQrCode inviteToken={team.inviteToken} />
        <div className="space-y-1 text-sm min-w-0">
          {editing ? (
            <form onSubmit={handleRename} className="space-y-2">
              <input className={`${INPUT} w-full`} value={name} onChange={(e) => setName(e.target.value)} autoFocus />
              <div className="flex gap-2">
                <button type="submit" disabled={busy || !name.trim()} className="rounded-lg bg-indigo-500 px-3 py-1.5 text-xs font-semibold disabled:opacity-40">
                  저장
                </button>
                <button type="button" onClick={() => { setEditing(false); setName(team.teamName); }} className={BUTTON_SUBTLE}>
                  취소
                </button>
              </div>
            </form>
          ) : (
            <p className="font-medium truncate">{team.teamName}</p>
          )}
          <p className="text-neutral-400 break-all">초대코드: {team.inviteToken}</p>
          <p className="text-neutral-500 text-xs">문제 {completed}/{total} 완료</p>
        </div>
      </div>
      {error && <p className="text-red-400 text-xs">{error}</p>}
      <div className="flex flex-wrap gap-2">
        <button onClick={() => setEditing(true)} disabled={busy || editing} className={BUTTON_SUBTLE}>이름 변경</button>
        <button onClick={handleRegenerate} disabled={busy} className={BUTTON_SUBTLE}>초대코드 재발급</button>
        <button
          onClick={handleDelete}
          disabled={busy}
          title={gameRunning ? "진행 중에는 진행 기록이 없는 팀만 삭제할 수 있습니다." : undefined}
          className={`${BUTTON_SUBTLE} text-red-400`}
        >
          삭제
        </button>
      </div>
    </div>
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
      setHint("");
      setOrderNo((n) => Math.max(n, quizzes.length) + 1);
      onChanged();
    } catch (e) {
      setError(errorMessage(e, "생성에 실패했습니다."));
    } finally {
      setCreating(false);
    }
  }

  return (
    <section className="space-y-3">
      <h2 className="font-semibold">문제 <span className="text-neutral-500 font-normal">({quizzes.length})</span></h2>

      <form onSubmit={handleCreate} className="space-y-2 rounded-xl bg-neutral-900 border border-neutral-800 p-4">
        <input className={`${INPUT} w-full`} placeholder="문제 제목" value={title} onChange={(e) => setTitle(e.target.value)} />
        <textarea
          className={`${INPUT} w-full`}
          placeholder="문제 내용"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={2}
        />
        <div className="flex gap-2">
          <select className={INPUT} value={type} onChange={(e) => setType(e.target.value as QuizType)}>
            <option value="TEXT">TEXT (정답 입력)</option>
            <option value="IMAGE">IMAGE (사진 제출)</option>
          </select>
          <label className="flex items-center gap-2 text-xs text-neutral-400">
            순서
            <input
              type="number"
              min={1}
              className={`${INPUT} w-20`}
              value={orderNo}
              onChange={(e) => setOrderNo(Number(e.target.value))}
            />
          </label>
        </div>
        {type === "TEXT" && (
          <input className={`${INPUT} w-full`} placeholder="정답" value={answer} onChange={(e) => setAnswer(e.target.value)} />
        )}
        <div className="flex gap-2">
          <input className={`${INPUT} flex-1`} placeholder="힌트 (선택)" value={hint} onChange={(e) => setHint(e.target.value)} />
          <label className="flex items-center gap-2 text-xs text-neutral-400 shrink-0">
            힌트 대기(초)
            <input
              type="number"
              min={0}
              className={`${INPUT} w-20`}
              value={hintDelaySeconds}
              onChange={(e) => setHintDelaySeconds(Number(e.target.value))}
            />
          </label>
        </div>
        {error && <p className="text-red-400 text-sm">{error}</p>}
        <button type="submit" disabled={creating} className="w-full rounded-lg bg-indigo-500 py-2 font-semibold disabled:opacity-40">
          문제 추가
        </button>
      </form>

      <ul className="space-y-2">
        {quizzes.map((quiz) => (
          <QuizItem key={quiz.id} quiz={quiz} onChanged={onChanged} />
        ))}
        {quizzes.length === 0 && <p className="text-neutral-500 text-sm">아직 문제가 없습니다.</p>}
      </ul>
    </section>
  );
}

function QuizItem({ quiz, onChanged }: { quiz: QuizAdmin; onChanged: () => void }) {
  const [confirm, dialog] = useConfirm();
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState(quiz.title);
  const [content, setContent] = useState(quiz.content);
  const [orderNo, setOrderNo] = useState(quiz.orderNo);
  const [hint, setHint] = useState(quiz.hint ?? "");
  const [hintDelaySeconds, setHintDelaySeconds] = useState(quiz.hintDelaySeconds);
  const [answer, setAnswer] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function startEditing() {
    setTitle(quiz.title);
    setContent(quiz.content);
    setOrderNo(quiz.orderNo);
    setHint(quiz.hint ?? "");
    setHintDelaySeconds(quiz.hintDelaySeconds);
    setAnswer("");
    setError(null);
    setEditing(true);
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await adminApi.updateQuiz(quiz.id, {
        title: title.trim(),
        content: content.trim(),
        orderNo,
        hint,
        hintDelaySeconds,
        answer: quiz.type === "TEXT" && answer.trim() ? answer.trim() : undefined,
      });
      setEditing(false);
      onChanged();
    } catch (e) {
      setError(errorMessage(e, "수정에 실패했습니다."));
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete() {
    if (!(await confirm(`'${quiz.title}' 문제를 삭제할까요?\n이 문제의 팀별 진행 기록도 함께 삭제됩니다.`, { confirmLabel: "삭제", danger: true }))) return;
    setBusy(true);
    setError(null);
    try {
      await adminApi.deleteQuiz(quiz.id);
      onChanged();
    } catch (e) {
      setError(errorMessage(e, "삭제에 실패했습니다."));
      setBusy(false);
    }
  }

  if (editing) {
    return (
      <li className="rounded-lg bg-neutral-900 border border-indigo-700/60 p-4">
        <form onSubmit={handleSave} className="space-y-2">
          <input className={`${INPUT} w-full`} value={title} onChange={(e) => setTitle(e.target.value)} />
          <textarea className={`${INPUT} w-full`} rows={3} value={content} onChange={(e) => setContent(e.target.value)} />
          <div className="flex items-center gap-2 text-xs text-neutral-400">
            <span className="rounded bg-neutral-800 px-2 py-1">{quiz.type} (유형은 변경할 수 없습니다)</span>
            <label className="flex items-center gap-2 ml-auto">
              순서
              <input type="number" min={1} className={`${INPUT} w-20`} value={orderNo} onChange={(e) => setOrderNo(Number(e.target.value))} />
            </label>
          </div>
          {quiz.type === "TEXT" && (
            <input
              className={`${INPUT} w-full`}
              placeholder={`정답 변경 시에만 입력 (현재: ${quiz.answer ?? "-"})`}
              value={answer}
              onChange={(e) => setAnswer(e.target.value)}
            />
          )}
          <div className="flex gap-2">
            <input className={`${INPUT} flex-1`} placeholder="힌트 (선택)" value={hint} onChange={(e) => setHint(e.target.value)} />
            <label className="flex items-center gap-2 text-xs text-neutral-400 shrink-0">
              힌트 대기(초)
              <input type="number" min={0} className={`${INPUT} w-20`} value={hintDelaySeconds} onChange={(e) => setHintDelaySeconds(Number(e.target.value))} />
            </label>
          </div>
          {error && <p className="text-red-400 text-sm">{error}</p>}
          <div className="flex gap-2">
            <button type="submit" disabled={busy} className="rounded-lg bg-indigo-500 px-4 py-1.5 text-sm font-semibold disabled:opacity-40">
              저장
            </button>
            <button type="button" onClick={() => setEditing(false)} className={BUTTON_SUBTLE}>
              취소
            </button>
          </div>
        </form>
      </li>
    );
  }

  return (
    <li className="rounded-lg bg-neutral-900 border border-neutral-800 px-4 py-3 text-sm">
      {dialog}
      <div className="flex items-center justify-between gap-2">
        <span className="font-medium">
          #{quiz.orderNo} {quiz.title}
        </span>
        <span className="text-xs text-neutral-500">{quiz.type}</span>
      </div>
      <p className="text-neutral-400 mt-1 whitespace-pre-wrap">{quiz.content}</p>
      {quiz.answer && <p className="text-neutral-500 text-xs mt-1">정답: {quiz.answer}</p>}
      {quiz.hint && (
        <p className="text-neutral-500 text-xs mt-1">
          힌트: {quiz.hint} ({quiz.hintDelaySeconds}초 후 공개)
        </p>
      )}
      {error && <p className="text-red-400 text-xs mt-2">{error}</p>}
      <div className="flex gap-2 mt-3">
        <button onClick={startEditing} disabled={busy} className={BUTTON_SUBTLE}>수정</button>
        <button onClick={handleDelete} disabled={busy} className={`${BUTTON_SUBTLE} text-red-400`}>삭제</button>
      </div>
    </li>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <main className="flex-1 flex items-center justify-center">{children}</main>;
}
