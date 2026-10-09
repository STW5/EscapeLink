import { API_BASE_URL } from "./api";
import type { ApiResponse } from "./types";
import type { PendingSubmission } from "./adminTypes";

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
    request<void>(`/api/admin/submissions/${submissionId}/reject`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reason }),
    }),

  imageUrl: (submissionId: number) =>
    `${API_BASE_URL}/api/admin/submissions/${submissionId}/image`,
};
