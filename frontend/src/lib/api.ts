import type { ApiResponse } from "./types";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  code: string;
  data: unknown;

  constructor(code: string, message: string, data: unknown) {
    super(message);
    this.code = code;
    this.data = data;
  }
}

async function request<T>(
  path: string,
  init?: RequestInit
): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });

  const body = (await res.json()) as ApiResponse<T>;

  if (!body.success) {
    throw new ApiError(body.code, body.message ?? "요청에 실패했습니다.", body.data);
  }

  return body.data as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path, { method: "GET" }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: "POST",
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }),
};

export { API_BASE_URL };
