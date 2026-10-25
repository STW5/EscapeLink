package com.stw.escapelink.admin.service;

/** Raised after games, teams or quizzes were edited or deleted; admin consoles refetch. */
public record AdminDataChangedEvent(String type, Long teamId, Long quizId) {
}
