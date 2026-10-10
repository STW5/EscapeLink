import { API_BASE_URL } from "./api";
import type { ApiResponse } from "./types";
import type { Game, PendingSubmission, QuizAdmin, TeamSummary } from "./adminTypes";

export class AdminApiError extends Error {
  code: string;

  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
  });
  const body = (await res.json()) as ApiResponse<T>;

  if (!body.success) {
    throw new AdminApiError(body.code, body.message ?? "요청에 실패했습니다.");
  }
  return body.data as T;
}

function postJson<T>(path: string, payload: unknown): Promise<T> {
  return request<T>(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

export const adminApi = {
  login: (username: string, password: string) =>
    request<{ username: string }>("/api/admin/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ username, password }).toString(),
    }),

  logout: () => request<void>("/api/admin/logout", { method: "POST" }),

  me: () => request<{ username: string }>("/api/admin/me"),

  listPending: () => request<PendingSubmission[]>("/api/admin/submissions"),

  approve: (submissionId: number) =>
    request<void>(`/api/admin/submissions/${submissionId}/approve`, { method: "POST" }),

  reject: (submissionId: number, reason: string) =>
    postJson<void>(`/api/admin/submissions/${submissionId}/reject`, { reason }),

  imageUrl: (submissionId: number) =>
    `${API_BASE_URL}/api/admin/submissions/${submissionId}/image`,

  listTeams: () => request<TeamSummary[]>("/api/admin/teams"),

  forceCompleteQuiz: (teamId: number, quizId: number) =>
    request<void>(`/api/admin/teams/${teamId}/quizzes/${quizId}/force-complete`, {
      method: "POST",
    }),

  resetTeam: (teamId: number) =>
    request<void>(`/api/admin/teams/${teamId}/reset`, { method: "POST" }),

  forceFinalStage: (teamId: number) =>
    request<void>(`/api/admin/teams/${teamId}/force-final-stage`, { method: "POST" }),

  listGames: () => request<Game[]>("/api/admin/games"),

  createGame: (title: string) => postJson<Game>("/api/admin/games", { title }),

  startGame: (gameId: number) =>
    request<Game>(`/api/admin/games/${gameId}/start`, { method: "POST" }),

  finishGame: (gameId: number) =>
    request<Game>(`/api/admin/games/${gameId}/finish`, { method: "POST" }),

  createTeam: (gameId: number, name: string) =>
    postJson<TeamSummary>(`/api/admin/games/${gameId}/teams`, { name }),

  listQuizzes: (gameId: number) =>
    request<QuizAdmin[]>(`/api/admin/games/${gameId}/quizzes`),

  createQuiz: (
    gameId: number,
    payload: {
      title: string;
      content: string;
      type: "TEXT" | "IMAGE";
      orderNo: number;
      hint: string;
      hintDelaySeconds: number;
      answer?: string;
    }
  ) => postJson<QuizAdmin>(`/api/admin/games/${gameId}/quizzes`, payload),
};
