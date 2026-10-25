"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { compressImage, UnsupportedImageError } from "@/lib/image";
import type { ImageSubmissionResult, QuizDetail, QuizProgressStatus } from "@/lib/types";

export default function ImageAnswerForm({
  quiz,
  onSubmitted,
}: {
  quiz: QuizDetail;
  onSubmitted: (status: QuizProgressStatus) => void;
}) {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setSelectedFile(file);
    setError(null);
    setPreviewUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return URL.createObjectURL(file);
    });
  }

  async function handleUpload() {
    if (!selectedFile) return;
    setUploading(true);
    setError(null);
    try {
      const compressed = await compressImage(selectedFile);
      const formData = new FormData();
      formData.append("file", compressed);

      const result = await api.postForm<ImageSubmissionResult>(
        `/api/quizzes/${quiz.id}/image`,
        formData
      );
      onSubmitted(result.status);
      setSelectedFile(null);
      setPreviewUrl((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return null;
      });
      // Clear both pickers so choosing the same photo again still fires a change event.
      for (const id of ["image-answer-camera", "image-answer-album"]) {
        const input = document.getElementById(id) as HTMLInputElement | null;
        if (input) input.value = "";
      }
    } catch (e) {
      setError(
        e instanceof ApiError || e instanceof UnsupportedImageError
          ? e.message
          : "업로드에 실패했습니다."
      );
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="space-y-3">
      {quiz.status === "UNSOLVED" && quiz.rejectReason && !selectedFile && (
        <div role="alert" className="rounded-lg bg-red-950/40 border border-red-800/60 px-4 py-3 text-sm space-y-1">
          <p className="font-semibold text-red-300">사진이 반려되었어요</p>
          <p className="text-red-200">사유: {quiz.rejectReason}</p>
          <p className="text-neutral-400">사유를 확인하고 다시 촬영해서 제출해 주세요.</p>
        </div>
      )}

      {quiz.status === "PENDING" && (
        <p className="text-amber-400 text-sm">
          관리자 검수를 기다리는 중입니다. 다시 제출하면 최신 제출본으로 교체됩니다.
        </p>
      )}

      {previewUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={previewUrl}
          alt="제출할 사진 미리보기"
          className="w-full rounded-lg border border-neutral-800"
        />
      )}

      {/* Two inputs: one opens the camera directly, the other lets the phone pick from the album. */}
      <input
        type="file"
        accept="image/*"
        capture="environment"
        onChange={handleFileChange}
        className="hidden"
        id="image-answer-camera"
      />
      <input
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        className="hidden"
        id="image-answer-album"
      />
      <div className="grid grid-cols-2 gap-2">
        <label
          htmlFor="image-answer-camera"
          className="block text-center rounded-lg bg-neutral-900 border border-neutral-700 py-3 cursor-pointer"
        >
          📷 {selectedFile ? "다시 촬영" : "카메라로 촬영"}
        </label>
        <label
          htmlFor="image-answer-album"
          className="block text-center rounded-lg bg-neutral-900 border border-neutral-700 py-3 cursor-pointer"
        >
          🖼️ 앨범에서 선택
        </label>
      </div>

      {error && <p className="text-red-400 text-sm">{error}</p>}

      <button
        onClick={handleUpload}
        disabled={!selectedFile || uploading}
        className="w-full rounded-lg bg-indigo-500 py-3 font-semibold disabled:opacity-40"
      >
        {uploading ? "업로드 중..." : "제출"}
      </button>
    </div>
  );
}
