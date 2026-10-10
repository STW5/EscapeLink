"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { adminApi, AdminApiError } from "@/lib/adminApi";
import type { TeamSummary } from "@/lib/adminTypes";
import TeamControlCard from "@/components/TeamControlCard";

export default function AdminTeamsPage() {
  const router = useRouter();
  const [teams, setTeams] = useState<TeamSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      await adminApi.me();
      const list = await adminApi.listTeams();
      setTeams(list);
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

  if (error) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-red-400">{error}</p>
      </main>
    );
  }

  if (!teams) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-neutral-400">불러오는 중...</p>
      </main>
    );
  }

  return (
    <main className="flex-1 px-5 py-8 max-w-2xl mx-auto w-full space-y-6">
      <header className="flex items-center justify-between">
        <h1 className="text-xl font-bold">팀 관리</h1>
        <div className="flex items-center gap-4">
          <a href="/admin/games" className="text-sm text-neutral-400">
            게임 관리
          </a>
          <a href="/admin/review" className="text-sm text-neutral-400">
            이미지 검수로
          </a>
        </div>
      </header>

      <div className="space-y-4">
        {teams.map((team) => (
          <TeamControlCard key={team.teamId} team={team} onChanged={load} />
        ))}
      </div>
    </main>
  );
}
