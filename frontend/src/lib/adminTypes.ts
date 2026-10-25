import type { GameStatus, QuizList, QuizType } from "./types";

export type PendingSubmission = {
  submissionId: number;
  teamId: number;
  teamName: string;
  quizId: number;
  quizTitle: string;
  submissionVersion: number;
  createdAt: string;
};

export type TeamSummary = {
  teamId: number;
  gameId: number;
  teamName: string;
  inviteToken: string;
  currentRunNo: number;
  finalStageUnlocked: boolean;
  finalStageForced: boolean;
  quizzes: QuizList;
};

export type Game = {
  id: number;
  title: string;
  status: GameStatus;
  startAt: string | null;
  endAt: string | null;
};

export type QuizAdmin = {
  id: number;
  gameId: number;
  title: string;
  content: string;
  type: QuizType;
  orderNo: number;
  hint: string | null;
  hintDelaySeconds: number;
  answer: string | null;
};

export type AdminActionType =
  | "APPROVE_IMAGE"
  | "REJECT_IMAGE"
  | "FORCE_COMPLETE"
  | "RESET_TEAM"
  | "FORCE_FINAL_STAGE";

export type AdminActionLog = {
  id: number;
  adminUsername: string;
  action: AdminActionType;
  teamId: number | null;
  teamName: string | null;
  quizId: number | null;
  quizTitle: string | null;
  detail: string | null;
  createdAt: string;
};
