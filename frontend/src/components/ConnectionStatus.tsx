"use client";

import { useOnlineStatus } from "@/lib/connectivity";

export default function ConnectionStatus() {
  const online = useOnlineStatus();

  if (online) {
    return null;
  }

  return (
    <div
      role="status"
      className="sticky top-0 z-50 bg-amber-500 px-4 py-2 text-center text-sm font-medium text-neutral-950"
    >
      오프라인 상태입니다. 연결되면 최신 진행 상황을 자동으로 불러옵니다.
    </div>
  );
}
