package com.stw.escapelink.global.websocket.dto;

public record ImageReviewedMessage(String type, Long quizId, String rejectReason) {

    public static ImageReviewedMessage approved(Long quizId) {
        return new ImageReviewedMessage("IMAGE_APPROVED", quizId, null);
    }

    public static ImageReviewedMessage rejected(Long quizId, String reason) {
        return new ImageReviewedMessage("IMAGE_REJECTED", quizId, reason);
    }
}
