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
  const isFormData = init?.body instanceof FormData;

  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      credentials: "include",
      headers: {
        // FormData sets its own multipart boundary; a manual Content-Type breaks it.
        ...(isFormData ? {} : { "Content-Type": "application/json" }),
        ...init?.headers,
      },
    });
  } catch {
    throw new ApiError(
      "NETWORK_ERROR",
      "네트워크 연결을 확인한 뒤 다시 시도해주세요.",
      null
    );
  }

  let body: ApiResponse<T>;
  try {
    body = (await res.json()) as ApiResponse<T>;
  } catch {
    throw new ApiError(
      "INVALID_RESPONSE",
      "서버 응답을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.",
      null
    );
  }

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
  postForm: <T>(path: string, formData: FormData) =>
    request<T>(path, { method: "POST", body: formData }),
};

export { API_BASE_URL };
