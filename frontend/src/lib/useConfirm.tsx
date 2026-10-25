"use client";

import { useCallback, useEffect, useRef, useState } from "react";

type Pending = { message: string; confirmLabel: string; danger: boolean };

/**
 * In-page replacement for window.confirm, which some in-app browsers block or
 * auto-dismiss. Usage: `const [confirm, dialog] = useConfirm();` then
 * `if (!(await confirm("..."))) return;` and render `{dialog}` once.
 */
export function useConfirm() {
  const [pending, setPending] = useState<Pending | null>(null);
  const resolver = useRef<((ok: boolean) => void) | null>(null);

  const confirm = useCallback(
    (message: string, options?: { confirmLabel?: string; danger?: boolean }) =>
      new Promise<boolean>((resolve) => {
        resolver.current?.(false);
        resolver.current = resolve;
        setPending({
          message,
          confirmLabel: options?.confirmLabel ?? "확인",
          danger: options?.danger ?? false,
        });
      }),
    []
  );

  const close = useCallback((ok: boolean) => {
    resolver.current?.(ok);
    resolver.current = null;
    setPending(null);
  }, []);

  useEffect(() => {
    if (!pending) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") close(false);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [pending, close]);

  const dialog = pending ? (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 px-6"
      onClick={() => close(false)}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        className="w-full max-w-sm rounded-2xl bg-neutral-900 border border-neutral-700 p-5 space-y-4"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="whitespace-pre-wrap text-sm leading-relaxed">{pending.message}</p>
        <div className="flex gap-2">
          <button
            onClick={() => close(false)}
            className="flex-1 rounded-lg bg-neutral-800 border border-neutral-700 py-2.5 text-sm"
          >
            취소
          </button>
          <button
            autoFocus
            onClick={() => close(true)}
            className={`flex-1 rounded-lg py-2.5 text-sm font-semibold ${
              pending.danger ? "bg-red-600" : "bg-indigo-500"
            }`}
          >
            {pending.confirmLabel}
          </button>
        </div>
      </div>
    </div>
  ) : null;

  return [confirm, dialog] as const;
}
