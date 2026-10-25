"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { adminApi, AdminApiError } from "@/lib/adminApi";
import { useAdminChannel } from "@/lib/ws";
import type { Game, TeamSummary } from "@/lib/adminTypes";
import AdminNav from "@/components/AdminNav";
import TeamControlCard from "@/components/TeamControlCard";

const STATUS_LABELS: Record<Game["status"], string> = {
  READY: "대기",
  RUNNING: "진행 중",
  FINISHED: "종료",
};

export default function AdminTeamsPage() {
  const router = useRouter();
  const [teams, setTeams] = useState<TeamSummary[] | null>(null);
  const [games, setGames] = useState<Game[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      await adminApi.me();
      const [list, gameList] = await Promise.all([adminApi.listTeams(), adminApi.listGames()]);
      setTeams(list);
      setGames(gameList);
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

  // Live updates: any committed change triggers a snapshot refetch.
  useAdminChannel(load);

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

  // Running games first, so the teams that matter during an event are on top.
  const order = { RUNNING: 0, READY: 1, FINISHED: 2 } as const;
  const groups = games
    .map((game) => ({ game, teams: teams.filter((t) => t.gameId === game.id) }))
    .filter((g) => g.teams.length > 0)
    .sort((a, b) => order[a.game.status] - order[b.game.status] || b.game.id - a.game.id);

  return (
    <main className="flex-1 px-5 py-6 max-w-2xl mx-auto w-full space-y-6">
      <AdminNav />
      <h1 className="text-xl font-bold">팀 현황</h1>

      {groups.length === 0 && <p className="text-center text-neutral-500 py-12">팀이 아직 없습니다.</p>}

      {groups.map(({ game, teams: gameTeams }) => (
        <section key={game.id} className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">
              {game.title}
              <span className="ml-2 text-xs font-normal text-neutral-500">{STATUS_LABELS[game.status]}</span>
            </h2>
            <a href={`/admin/game?id=${game.id}`} className="text-xs text-neutral-500">
              관리 →
            </a>
          </div>
          {gameTeams.map((team) => (
            <TeamControlCard key={team.teamId} team={team} onChanged={load} />
          ))}
        </section>
      ))}
    </main>
  );
}
