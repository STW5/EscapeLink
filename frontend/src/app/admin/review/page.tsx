"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { adminApi, AdminApiError } from "@/lib/adminApi";
import type { PendingSubmission } from "@/lib/adminTypes";
import SubmissionCard from "@/components/SubmissionCard";

export default function AdminReviewPage() {
  const router = useRouter();
  const [username, setUsername] = useState<string | null>(null);
  const [submissions, setSubmissions] = useState<PendingSubmission[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const me = await adminApi.me();
      setUsername(me.username);
      const pending = await adminApi.listPending();
      setSubmissions(pending);
      setError(null);
    } catch (e) {
      if (e instanceof AdminApiError) {
        router.push("/admin/login");
        return;
      }
      setError("불러오지 못했습니다.");
    }
  }, [router]);

  useEffect(() => {
    load();
  }, [load]);

  function handleResolved(submissionId: number) {
    setSubmissions((prev) => prev?.filter((s) => s.submissionId !== submissionId) ?? null);
  }

  async function handleLogout() {
    await adminApi.logout().catch(() => {});
    router.push("/admin/login");
  }

  if (error) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-red-400">{error}</p>
      </main>
    );
  }

  if (!submissions) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-neutral-400">불러오는 중...</p>
      </main>
    );
  }

  return (
    <main className="flex-1 px-5 py-8 max-w-2xl mx-auto w-full space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">이미지 검수</h1>
          <p className="text-sm text-neutral-400">{username} 로그인 중</p>
        </div>
        <button onClick={handleLogout} className="text-sm text-neutral-400">
          로그아웃
        </button>
      </header>

      {submissions.length === 0 ? (
        <p className="text-center text-neutral-500 py-12">검수 대기 중인 제출물이 없습니다.</p>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2">
          {submissions.map((submission) => (
            <SubmissionCard
              key={submission.submissionId}
              submission={submission}
              onResolved={handleResolved}
            />
          ))}
        </div>
      )}
    </main>
  );
}
