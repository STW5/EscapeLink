"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { adminApi, AdminApiError } from "@/lib/adminApi";
import type { AdminActionLog, AdminActionType } from "@/lib/adminTypes";
import { useAdminChannel } from "@/lib/ws";
import AdminNav from "@/components/AdminNav";

const ACTION_LABELS: Record<AdminActionType, string> = {
  APPROVE_IMAGE: "이미지 승인",
  REJECT_IMAGE: "이미지 반려",
  FORCE_COMPLETE: "퀴즈 강제 완료",
  RESET_TEAM: "팀 초기화",
  FORCE_FINAL_STAGE: "최종 미션 강제 오픈",
  UPDATE_GAME: "게임 수정",
  DELETE_GAME: "게임 삭제",
  UPDATE_TEAM: "팀 이름 변경",
  DELETE_TEAM: "팀 삭제",
  REGENERATE_INVITE: "초대코드 재발급",
  UPDATE_QUIZ: "문제 수정",
  DELETE_QUIZ: "문제 삭제",
};

export default function AdminLogsPage() {
  const router = useRouter();
  const [logs, setLogs] = useState<AdminActionLog[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      await adminApi.me();
      setLogs(await adminApi.listActionLogs());
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

  useAdminChannel(load);

  if (error) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-red-400">{error}</p>
      </main>
    );
  }

  if (!logs) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-neutral-400">불러오는 중...</p>
      </main>
    );
  }

  return (
    <main className="flex-1 px-5 py-6 max-w-2xl mx-auto w-full space-y-6">
      <AdminNav />
      <h1 className="text-xl font-bold">액션 로그</h1>

      {logs.length === 0 ? (
        <p className="text-center text-neutral-500 py-12">기록된 액션이 없습니다.</p>
      ) : (
        <ul className="space-y-2">
          {logs.map((log) => (
            <li key={log.id} className="rounded-lg bg-neutral-900 px-4 py-3 text-sm space-y-1">
              <div className="flex items-center justify-between gap-3">
                <span className="font-semibold">{ACTION_LABELS[log.action] ?? log.action}</span>
                <time className="text-xs text-neutral-500">
                  {new Date(log.createdAt).toLocaleString("ko-KR")}
                </time>
              </div>
              <p className="text-neutral-400">
                {log.adminUsername}
                {log.teamName && ` · ${log.teamName}`}
                {log.quizTitle && ` · ${log.quizTitle}`}
                {log.detail && ` · ${log.detail}`}
              </p>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
