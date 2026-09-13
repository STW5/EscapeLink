package com.stw.escapelink.global.websocket.dto;

public record QuizCompletedMessage(String type, Long quizId, long stateVersion) {

    public static QuizCompletedMessage of(Long quizId, long stateVersion) {
        return new QuizCompletedMessage("QUIZ_COMPLETED", quizId, stateVersion);
    }
}
