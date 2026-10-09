package com.stw.escapelink.admin.service;

/** Raised only after the review transaction commits. rejectReason is null when approved. */
public record ImageReviewedEvent(Long teamId, Long quizId, boolean approved, String rejectReason) {
}
