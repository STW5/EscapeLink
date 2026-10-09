"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { adminApi, AdminApiError } from "@/lib/adminApi";

export default function AdminLoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await adminApi.login(username, password);
      router.push("/admin/review");
    } catch (e) {
      setError(e instanceof AdminApiError ? e.message : "로그인에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="flex-1 flex items-center justify-center px-6">
      <form onSubmit={handleSubmit} className="w-full max-w-sm space-y-4">
        <h1 className="text-2xl font-bold text-center mb-6">EscapeLink 관리자</h1>
        <input
          className="w-full rounded-lg bg-neutral-900 border border-neutral-700 px-4 py-3"
          placeholder="아이디"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
        />
        <input
          type="password"
          className="w-full rounded-lg bg-neutral-900 border border-neutral-700 px-4 py-3"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
        />
        {error && <p className="text-red-400 text-sm text-center">{error}</p>}
        <button
          type="submit"
          disabled={submitting || !username || !password}
          className="w-full rounded-lg bg-indigo-500 py-3 font-semibold disabled:opacity-40"
        >
          {submitting ? "로그인 중..." : "로그인"}
        </button>
      </form>
    </main>
  );
}
