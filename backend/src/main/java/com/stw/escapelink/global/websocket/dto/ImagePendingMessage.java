package com.stw.escapelink.global.websocket.dto;

public record ImagePendingMessage(String type, Long quizId, Long submissionId) {

    public static ImagePendingMessage of(Long quizId, Long submissionId) {
        return new ImagePendingMessage("IMAGE_PENDING", quizId, submissionId);
    }
}
