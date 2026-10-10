export type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string | null;
  data: T | null;
};

export type GameStatus = "READY" | "RUNNING" | "FINISHED";

export type TeamState = {
  teamId: number;
  teamName: string;
  currentRunNo: number;
  gameId: number;
  gameStatus: GameStatus;
  gameStartAt: string | null;
  gameEndAt: string | null;
  finalStageUnlocked: boolean;
};

export type QuizType = "TEXT" | "IMAGE";
export type QuizProgressStatus = "UNSOLVED" | "PENDING" | "COMPLETED";

export type QuizListItem = {
  id: number;
  title: string;
  type: QuizType;
  status: QuizProgressStatus;
};

export type QuizList = {
  gameId: number;
  teamId: number;
  quizzes: QuizListItem[];
};

export type QuizDetail = {
  id: number;
  title: string;
  content: string;
  type: QuizType;
  status: QuizProgressStatus;
  hintAvailable: boolean;
  hintAvailableAt: string | null;
  hint: string | null;
};

export type AnswerResult = {
  correct: boolean;
  status: QuizProgressStatus;
  solvedAt: string | null;
};

export type ImageSubmissionResult = {
  submissionId: number;
  submissionVersion: number;
  status: QuizProgressStatus;
};

export type QuizCompletedMessage = {
  type: "QUIZ_COMPLETED";
  quizId: number;
  stateVersion: number;
};

export type ImagePendingMessage = {
  type: "IMAGE_PENDING";
  quizId: number;
  submissionId: number;
};

export type TeamEvent = QuizCompletedMessage | ImagePendingMessage;
