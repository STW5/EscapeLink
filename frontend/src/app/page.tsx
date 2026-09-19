"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import type { TeamState } from "@/lib/types";

export default function Home() {
  const router = useRouter();

  useEffect(() => {
    api
      .get<TeamState>("/api/team-state")
      .then(() => router.replace("/quizzes"))
      .catch((e) => {
        if (e instanceof ApiError) {
          router.replace("/join");
        }
      });
  }, [router]);

  return (
    <main className="flex-1 flex items-center justify-center">
      <p className="text-neutral-400">불러오는 중...</p>
    </main>
  );
}
