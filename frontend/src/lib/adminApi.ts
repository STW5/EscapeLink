import { API_BASE_URL } from "./api";
import type { ApiResponse } from "./types";
import type { AdminActionLog, Game, PendingSubmission, QuizAdmin, TeamSummary } from "./adminTypes";

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

function sendJson<T>(method: "PUT", path: string, payload: unknown): Promise<T> {
  return request<T>(path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
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

  listActionLogs: (teamId?: number) =>
    request<AdminActionLog[]>(
      `/api/admin/action-logs${teamId != null ? `?teamId=${teamId}` : ""}`
    ),

  updateGame: (
    gameId: number,
    payload: { title: string; startAt: string | null; endAt: string | null }
  ) => sendJson<Game>("PUT", `/api/admin/games/${gameId}`, payload),

  deleteGame: (gameId: number) => request<void>(`/api/admin/games/${gameId}`, { method: "DELETE" }),

  renameTeam: (teamId: number, name: string) =>
    sendJson<TeamSummary>("PUT", `/api/admin/teams/${teamId}`, { name }),

  regenerateInvite: (teamId: number) =>
    request<TeamSummary>(`/api/admin/teams/${teamId}/regenerate-invite`, { method: "POST" }),

  deleteTeam: (teamId: number) => request<void>(`/api/admin/teams/${teamId}`, { method: "DELETE" }),

  updateQuiz: (
    quizId: number,
    payload: {
      title: string;
      content: string;
      orderNo: number;
      hint: string;
      hintDelaySeconds: number;
      answer?: string;
    }
  ) => sendJson<QuizAdmin>("PUT", `/api/admin/quizzes/${quizId}`, payload),

  deleteQuiz: (quizId: number) => request<void>(`/api/admin/quizzes/${quizId}`, { method: "DELETE" }),

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
