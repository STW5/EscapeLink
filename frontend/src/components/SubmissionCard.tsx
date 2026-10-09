"use client";

import { useState } from "react";
import { adminApi, AdminApiError } from "@/lib/adminApi";
import type { PendingSubmission } from "@/lib/adminTypes";

const REASON_PRESETS = ["얼굴 식별 불가", "미션 주제와 상이", "초점 흐림"];

export default function SubmissionCard({
  submission,
  onResolved,
}: {
  submission: PendingSubmission;
  onResolved: (submissionId: number) => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showReject, setShowReject] = useState(false);
  const [reason, setReason] = useState("");

  async function handleApprove() {
    setBusy(true);
    setError(null);
    try {
      await adminApi.approve(submission.submissionId);
      onResolved(submission.submissionId);
    } catch (e) {
      setError(e instanceof AdminApiError ? e.message : "승인에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  }

  async function handleReject() {
    if (!reason.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await adminApi.reject(submission.submissionId, reason.trim());
      onResolved(submission.submissionId);
    } catch (e) {
      setError(e instanceof AdminApiError ? e.message : "반려에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="rounded-xl bg-neutral-900 border border-neutral-800 overflow-hidden">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={adminApi.imageUrl(submission.submissionId)}
        alt={`${submission.teamName} 제출 사진`}
        className="w-full max-h-96 object-contain bg-black"
      />
      <div className="p-4 space-y-3">
        <div className="flex items-center justify-between">
          <span className="font-semibold">{submission.teamName}</span>
          <span className="text-sm text-neutral-400">{submission.quizTitle}</span>
        </div>
        <p className="text-xs text-neutral-500">
          제출 #{submission.submissionVersion} ·{" "}
          {new Date(submission.createdAt).toLocaleString("ko-KR")}
        </p>

        {error && <p className="text-red-400 text-sm">{error}</p>}

        {!showReject ? (
          <div className="flex gap-2">
            <button
              onClick={handleApprove}
              disabled={busy}
              className="flex-1 rounded-lg bg-emerald-600 py-2 font-semibold disabled:opacity-40"
            >
              승인
            </button>
            <button
              onClick={() => setShowReject(true)}
              disabled={busy}
              className="flex-1 rounded-lg bg-red-600 py-2 font-semibold disabled:opacity-40"
            >
              반려
            </button>
          </div>
        ) : (
          <div className="space-y-2">
            <div className="flex flex-wrap gap-2">
              {REASON_PRESETS.map((preset) => (
                <button
                  key={preset}
                  onClick={() => setReason(preset)}
                  className={`text-xs px-2.5 py-1 rounded-full border ${
                    reason === preset
                      ? "bg-red-600 border-red-600"
                      : "border-neutral-700 text-neutral-300"
                  }`}
                >
                  {preset}
                </button>
              ))}
            </div>
            <input
              className="w-full rounded-lg bg-neutral-950 border border-neutral-700 px-3 py-2 text-sm"
              placeholder="반려 사유 입력"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            <div className="flex gap-2">
              <button
                onClick={() => setShowReject(false)}
                className="flex-1 rounded-lg bg-neutral-800 py-2 text-sm"
              >
                취소
              </button>
              <button
                onClick={handleReject}
                disabled={busy || !reason.trim()}
                className="flex-1 rounded-lg bg-red-600 py-2 text-sm font-semibold disabled:opacity-40"
              >
                반려 확정
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
