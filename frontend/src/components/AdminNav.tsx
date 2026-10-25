"use client";

import { useCallback, useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { adminApi } from "@/lib/adminApi";
import { useAdminChannel } from "@/lib/ws";

const LINKS = [
  { href: "/admin/games", label: "게임", match: ["/admin/games", "/admin/game"] },
  { href: "/admin/teams", label: "팀 현황", match: ["/admin/teams"] },
  { href: "/admin/review", label: "이미지 검수", match: ["/admin/review"] },
  { href: "/admin/logs", label: "액션 로그", match: ["/admin/logs"] },
];

/** Shared admin navigation; shows how many image submissions are waiting for review. */
export default function AdminNav() {
  const pathname = usePathname()?.replace(/\/$/, "") ?? "";
  const router = useRouter();
  const [pending, setPending] = useState(0);

  const loadPending = useCallback(() => {
    adminApi
      .listPending()
      .then((list) => setPending(list.length))
      .catch(() => {});
  }, []);

  useEffect(() => {
    loadPending();
  }, [loadPending]);
  useAdminChannel(loadPending);

  async function handleLogout() {
    await adminApi.logout().catch(() => {});
    router.push("/admin/login");
  }

  return (
    <nav className="flex items-center gap-1 overflow-x-auto -mx-1 pb-1 text-sm">
      {LINKS.map((link) => {
        const active = link.match.includes(pathname);
        return (
          <a
            key={link.href}
            href={link.href}
            className={`whitespace-nowrap rounded-lg px-3 py-1.5 ${
              active ? "bg-neutral-800 text-white" : "text-neutral-400"
            }`}
          >
            {link.label}
            {link.href === "/admin/review" && pending > 0 && (
              <span className="ml-1.5 rounded-full bg-red-600 px-1.5 py-0.5 text-xs font-semibold text-white">
                {pending}
              </span>
            )}
          </a>
        );
      })}
      <button onClick={handleLogout} className="ml-auto whitespace-nowrap px-3 py-1.5 text-neutral-500">
        로그아웃
      </button>
    </nav>
  );
}
