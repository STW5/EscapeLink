"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import type { AnswerResult, QuizDetail, QuizProgressStatus, TeamState } from "@/lib/types";
import StatusBadge from "@/components/StatusBadge";
import ImageAnswerForm from "@/components/ImageAnswerForm";

const DEFAULT_COOLDOWN_SECONDS = 10;

export default function QuizPage() {
  return (
    <Suspense fallback={<Centered>불러오는 중...</Centered>}>
      <QuizDetailView />
    </Suspense>
  );
}

function QuizDetailView() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const quizId = searchParams.get("id");

  const [teamState, setTeamState] = useState<TeamState | null>(null);
  const [quiz, setQuiz] = useState<QuizDetail | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [answer, setAnswer] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<
    { kind: "wrong" | "error"; message: string } | null
  >(null);
  const [cooldownUntil, setCooldownUntil] = useState<number | null>(null);
  const [cooldownSecondsLeft, setCooldownSecondsLeft] = useState(0);

  const load = useCallback(async () => {
    if (!quizId) return;
    try {
      const [state, detail] = await Promise.all([
        api.get<TeamState>("/api/team-state"),
        api.get<QuizDetail>(`/api/quizzes/${quizId}`),
      ]);
      setTeamState(state);
      setQuiz(detail);
      setLoadError(null);
    } catch (e) {
      if (e instanceof ApiError && e.code === "SESSION_EXPIRED") {
        router.push("/join");
        return;
      }
      setLoadError(e instanceof ApiError ? e.message : "불러오지 못했습니다.");
    }
  }, [quizId, router]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!cooldownUntil) return;
    const tick = () => {
      const secondsLeft = Math.max(
        Math.ceil((cooldownUntil - Date.now()) / 1000),
        0
      );
      setCooldownSecondsLeft(secondsLeft);
      if (secondsLeft <= 0) {
        setCooldownUntil(null);
      }
    };
    tick();
    const id = setInterval(tick, 250);
    return () => clearInterval(id);
  }, [cooldownUntil]);

  async function submitAnswer(e: React.FormEvent) {
    e.preventDefault();
    if (!quiz || !teamState || !answer.trim()) return;

    setSubmitting(true);
    setFeedback(null);
    try {
      const result = await api.post<AnswerResult>(
        `/api/quizzes/${quiz.id}/answer`,
        {
          requestId: crypto.randomUUID(),
          answer: answer.trim(),
          runNo: teamState.currentRunNo,
        }
      );
      setQuiz({ ...quiz, status: result.status });
      setAnswer("");
    } catch (e) {
      if (e instanceof ApiError && e.code === "WRONG_ANSWER") {
        setFeedback({ kind: "wrong", message: "정답이 아닙니다." });
        setCooldownUntil(Date.now() + DEFAULT_COOLDOWN_SECONDS * 1000);
      } else if (e instanceof ApiError && e.code === "QUIZ_COOLDOWN") {
        const retryAfterSeconds =
          (e.data as { retryAfterSeconds?: number } | null)
            ?.retryAfterSeconds ?? DEFAULT_COOLDOWN_SECONDS;
        setCooldownUntil(Date.now() + retryAfterSeconds * 1000);
      } else if (e instanceof ApiError && e.code === "QUIZ_ALREADY_COMPLETED") {
        setQuiz({ ...quiz, status: "COMPLETED" });
      } else {
        setFeedback({
          kind: "error",
          message: e instanceof ApiError ? e.message : "제출에 실패했습니다.",
        });
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (loadError) {
    return <Centered><p className="text-red-400">{loadError}</p></Centered>;
  }
  if (!quiz) {
    return <Centered>불러오는 중...</Centered>;
  }

  const solved = quiz.status === "COMPLETED";
  const disabled = submitting || solved || cooldownSecondsLeft > 0;

  return (
    <main className="flex-1 px-5 py-8 max-w-md mx-auto w-full space-y-6">
      <button
        onClick={() => router.push("/quizzes")}
        className="text-sm text-neutral-400"
      >
        ← 목록으로
      </button>

      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold">{quiz.title}</h1>
          <StatusBadge status={quiz.status} />
        </div>
        <p className="whitespace-pre-wrap text-neutral-200 leading-relaxed">
          {quiz.content}
        </p>
      </div>

      <HintBox quiz={quiz} />

      {solved ? (
        <p className="text-center text-emerald-400 font-medium py-4">
          이미 완료한 문제입니다.
        </p>
      ) : quiz.type === "IMAGE" ? (
        <ImageAnswerForm
          quiz={quiz}
          onSubmitted={(status: QuizProgressStatus) =>
            setQuiz((prev) => (prev ? { ...prev, status } : prev))
          }
        />
      ) : (
        <form onSubmit={submitAnswer} className="space-y-3">
          <input
            className="w-full rounded-lg bg-neutral-900 border border-neutral-700 px-4 py-3"
            placeholder="정답 입력"
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            disabled={disabled}
          />
          {feedback && (
            <p
              className={
                feedback.kind === "wrong"
                  ? "text-amber-400 text-sm"
                  : "text-red-400 text-sm"
              }
            >
              {feedback.message}
            </p>
          )}
          <button
            type="submit"
            disabled={disabled || !answer.trim()}
            className="w-full rounded-lg bg-indigo-500 py-3 font-semibold disabled:opacity-40"
          >
            {cooldownSecondsLeft > 0
              ? `${cooldownSecondsLeft}초 후 재시도`
              : "제출"}
          </button>
        </form>
      )}
    </main>
  );
}

function HintBox({ quiz }: { quiz: QuizDetail }) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (quiz.hintAvailable || !quiz.hintAvailableAt) return;
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, [quiz.hintAvailable, quiz.hintAvailableAt]);

  if (quiz.hintAvailable) {
    return (
      <div className="rounded-lg bg-neutral-900 border border-neutral-800 px-4 py-3 text-sm">
        <p className="text-neutral-400 mb-1">힌트</p>
        <p>{quiz.hint}</p>
      </div>
    );
  }

  if (!quiz.hintAvailableAt) return null;

  const secondsLeft = Math.max(
    Math.ceil((new Date(quiz.hintAvailableAt).getTime() - now) / 1000),
    0
  );

  return (
    <p className="text-sm text-neutral-500">
      힌트는 {secondsLeft}초 후 확인할 수 있습니다.
    </p>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return (
    <main className="flex-1 flex items-center justify-center">{children}</main>
  );
}
