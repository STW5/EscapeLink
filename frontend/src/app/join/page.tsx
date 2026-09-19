"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { getDeviceId } from "@/lib/device";
import type { TeamState } from "@/lib/types";

export default function JoinPage() {
  return (
    <Suspense fallback={<Centered>불러오는 중...</Centered>}>
      <JoinForm />
    </Suspense>
  );
}

function JoinForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const tokenFromUrl = searchParams.get("token") ?? "";

  const [inviteToken, setInviteToken] = useState(tokenFromUrl);
  const [status, setStatus] = useState<"idle" | "joining" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function join(token: string) {
    if (!token.trim()) return;
    setStatus("joining");
    setErrorMessage(null);
    try {
      const state = await api.post<TeamState>("/api/team-sessions/join", {
        inviteToken: token.trim(),
        deviceId: getDeviceId(),
      });
      window.localStorage.setItem("escapelink_team_name", state.teamName);
      router.push("/quizzes");
    } catch (e) {
      setStatus("error");
      setErrorMessage(e instanceof ApiError ? e.message : "접속에 실패했습니다.");
    }
  }

  useEffect(() => {
    if (tokenFromUrl) {
      join(tokenFromUrl);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tokenFromUrl]);

  return (
    <Centered>
      <div className="w-full max-w-sm px-6 py-10 space-y-6">
        <div className="text-center space-y-1">
          <h1 className="text-2xl font-bold">EscapeLink</h1>
          <p className="text-neutral-400 text-sm">
            QR 코드를 스캔했다면 자동으로 접속됩니다.
          </p>
        </div>

        {status === "joining" && (
          <p className="text-center text-neutral-300">팀 접속 중...</p>
        )}

        {status === "error" && errorMessage && (
          <p className="text-center text-red-400 text-sm">{errorMessage}</p>
        )}

        <form
          className="space-y-3"
          onSubmit={(e) => {
            e.preventDefault();
            join(inviteToken);
          }}
        >
          <input
            className="w-full rounded-lg bg-neutral-900 border border-neutral-700 px-4 py-3 text-center tracking-wide"
            placeholder="초대 코드 직접 입력"
            value={inviteToken}
            onChange={(e) => setInviteToken(e.target.value)}
          />
          <button
            type="submit"
            disabled={status === "joining" || !inviteToken.trim()}
            className="w-full rounded-lg bg-indigo-500 py-3 font-semibold disabled:opacity-40"
          >
            팀 접속하기
          </button>
        </form>
      </div>
    </Centered>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return (
    <main className="flex-1 flex items-center justify-center">{children}</main>
  );
}
