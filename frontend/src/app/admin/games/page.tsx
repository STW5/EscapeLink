"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { adminApi, AdminApiError } from "@/lib/adminApi";
import type { Game } from "@/lib/adminTypes";

const STATUS_LABELS: Record<Game["status"], string> = {
  READY: "대기",
  RUNNING: "진행 중",
  FINISHED: "종료",
};

export default function AdminGamesPage() {
  const router = useRouter();
  const [games, setGames] = useState<Game[] | null>(null);
  const [title, setTitle] = useState("");
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      await adminApi.me();
      setGames(await adminApi.listGames());
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

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setCreating(true);
    setError(null);
    try {
      await adminApi.createGame(title.trim());
      setTitle("");
      await load();
    } catch (e) {
      setError(e instanceof AdminApiError ? e.message : "생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  }

  if (!games) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-neutral-400">불러오는 중...</p>
      </main>
    );
  }

  return (
    <main className="flex-1 px-5 py-8 max-w-xl mx-auto w-full space-y-6">
      <header className="flex items-center justify-between">
        <h1 className="text-xl font-bold">게임 관리</h1>
        <div className="flex items-center gap-4 text-sm text-neutral-400">
          <a href="/admin/teams">팀 관리</a>
          <a href="/admin/review">이미지 검수</a>
        </div>
      </header>

      <form onSubmit={handleCreate} className="flex gap-2">
        <input
          className="flex-1 rounded-lg bg-neutral-900 border border-neutral-700 px-4 py-2"
          placeholder="새 게임 이름"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <button
          type="submit"
          disabled={creating || !title.trim()}
          className="rounded-lg bg-indigo-500 px-4 py-2 font-semibold disabled:opacity-40"
        >
          생성
        </button>
      </form>

      {error && <p className="text-red-400 text-sm">{error}</p>}

      <ul className="space-y-2">
        {games.map((game) => (
          <li key={game.id}>
            <button
              onClick={() => router.push(`/admin/game?id=${game.id}`)}
              className="w-full flex items-center justify-between rounded-xl bg-neutral-900 border border-neutral-800 px-4 py-3 text-left"
            >
              <span className="font-medium">{game.title}</span>
              <span className="text-xs text-neutral-400">{STATUS_LABELS[game.status]}</span>
            </button>
          </li>
        ))}
        {games.length === 0 && (
          <p className="text-center text-neutral-500 py-8">게임이 아직 없습니다.</p>
        )}
      </ul>
    </main>
  );
}
