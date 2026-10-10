import type { QuizList } from "./types";

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
  teamName: string;
  inviteToken: string;
  currentRunNo: number;
  finalStageUnlocked: boolean;
  finalStageForced: boolean;
  quizzes: QuizList;
};
