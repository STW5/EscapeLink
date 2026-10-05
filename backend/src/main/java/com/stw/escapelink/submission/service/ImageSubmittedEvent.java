package com.stw.escapelink.submission.service;

/** Raised only after the submission-creating transaction commits. */
public record ImageSubmittedEvent(Long teamId, Long quizId, Long submissionId) {
}
