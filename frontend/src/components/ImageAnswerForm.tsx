"use client";

import { useRef, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { compressImage } from "@/lib/image";
import type { ImageSubmissionResult, QuizDetail, QuizProgressStatus } from "@/lib/types";

export default function ImageAnswerForm({
  quiz,
  onSubmitted,
}: {
  quiz: QuizDetail;
  onSubmitted: (status: QuizProgressStatus) => void;
}) {
  const fileInputRef = useRef<HTMLInputElement>(null);
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
      if (fileInputRef.current) fileInputRef.current.value = "";
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "업로드에 실패했습니다.");
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="space-y-3">
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

      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        capture="environment"
        onChange={handleFileChange}
        className="hidden"
        id="image-answer-input"
      />
      <label
        htmlFor="image-answer-input"
        className="block w-full text-center rounded-lg bg-neutral-900 border border-neutral-700 py-3 cursor-pointer"
      >
        {selectedFile ? "다른 사진 선택" : "사진 선택 / 촬영"}
      </label>

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
