package com.stw.escapelink.quiz.service;

/** Raised only after the completing transaction commits; WebSocket is notify-only, never authoritative. */
public record QuizCompletedEvent(Long teamId, Long quizId, long stateVersion) {
}
