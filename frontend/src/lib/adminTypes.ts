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
