import type { QuizProgressStatus } from "@/lib/types";

const STYLES: Record<QuizProgressStatus, string> = {
  UNSOLVED: "bg-neutral-800 text-neutral-300",
  PENDING: "bg-amber-900/60 text-amber-300",
  COMPLETED: "bg-emerald-900/60 text-emerald-300",
};

const LABELS: Record<QuizProgressStatus, string> = {
  UNSOLVED: "미해결",
  PENDING: "검수 중",
  COMPLETED: "완료",
};

export default function StatusBadge({ status }: { status: QuizProgressStatus }) {
  return (
    <span className={`text-xs font-medium px-2.5 py-1 rounded-full ${STYLES[status]}`}>
      {LABELS[status]}
    </span>
  );
}
